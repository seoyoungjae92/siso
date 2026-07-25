package com.siso.backend.moderation;

import com.siso.backend.comment.Comment;
import com.siso.backend.comment.Report;
import com.siso.backend.comment.ReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * D11 — 대기중인 신고를 Claude Haiku로 1차 분류해 어드민 신고 큐에 참고용
 * 힌트(obvious_violation/ambiguous)를 붙인다. 자동 블라인드·이의제기 같은
 * 자동 조치는 하지 않는다 — 오탐으로 실제 댓글이 잘못 가려지는 리스크를
 * 피하기 위해 힌트만 제공하고 최종 판단은 여전히 사람(AdminReportService)이
 * 한다.
 *
 * 개별 댓글 분류 실패(API 에러, 응답 형식 이상 등)는 건너뛰고 나머지를
 * 계속 처리한다(크롤러/PetitionSyncService와 동일한 "개별 실패는 스킵"
 * 원칙) — 다음 배치에서 다시 시도된다(llm_verdict가 여전히 null이므로).
 */
@Service
public class ReportClassificationService {

    private static final String PENDING = "pending";
    private static final Set<String> VALID_VERDICTS = Set.of("obvious_violation", "ambiguous");
    private static final int BATCH_SIZE = 20;

    private final ReportRepository reportRepository;
    private final AnthropicReportClassifier classifier;

    public ReportClassificationService(ReportRepository reportRepository, AnthropicReportClassifier classifier) {
        this.reportRepository = reportRepository;
        this.classifier = classifier;
    }

    @Transactional
    public int classifyPending() {
        if (!classifier.isEnabled()) {
            return 0;
        }

        List<Comment> candidates =
                reportRepository.findDistinctCommentsWithUnclassifiedPendingReports(PageRequest.of(0, BATCH_SIZE));

        int classified = 0;
        for (Comment comment : candidates) {
            if (classifyOne(comment)) {
                classified++;
            }
        }
        return classified;
    }

    private boolean classifyOne(Comment comment) {
        List<Report> reports = reportRepository.findByStatusAndComment_Id(PENDING, comment.getId());
        if (reports.isEmpty()) {
            return false;
        }

        List<String> reasons = reports.stream().map(Report::getReason).toList();
        List<String> details = reports.stream().map(Report::getDetail).filter(Objects::nonNull).toList();

        ReportClassification result;
        try {
            result = classifier.classify(comment.getBody(), reasons, details);
        } catch (ReportClassificationFailed e) {
            return false;
        }

        if (!VALID_VERDICTS.contains(result.verdict())) {
            return false;
        }

        comment.applyLlmClassification(result.verdict(), result.reason(), OffsetDateTime.now());
        return true;
    }
}
