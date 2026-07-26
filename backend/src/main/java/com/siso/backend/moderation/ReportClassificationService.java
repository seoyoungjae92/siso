package com.siso.backend.moderation;

import com.siso.backend.comment.Comment;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.Report;
import com.siso.backend.comment.ReportRepository;
import com.siso.backend.settings.ModerationSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * D11 — 대기중인 신고를 LLM(OpenRouter 경유, 기본 openrouter/free — 어드민이
 * moderation_settings.classificationModel로 특정 모델로 전환 가능)으로 1차
 * 분류해 어드민 신고 큐에 참고용 힌트(obvious_violation/ambiguous)를 붙인다.
 * 자동 블라인드·이의제기 같은 자동 조치는 하지 않는다 — 오탐으로 실제 댓글이 잘못 가려지는 리스크를
 * 피하기 위해 힌트만 제공하고 최종 판단은 여전히 사람(AdminReportService)이
 * 한다.
 *
 * 개별 댓글 분류 실패(API 에러, 응답 형식 이상 등)는 건너뛰고 나머지를
 * 계속 처리한다(크롤러/PetitionSyncService와 동일한 "개별 실패는 스킵"
 * 원칙) — 다음 배치에서 다시 시도된다(llm_verdict가 여전히 null이므로).
 */
@Service
public class ReportClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ReportClassificationService.class);
    private static final String PENDING = "pending";
    private static final Set<String> VALID_VERDICTS = Set.of("obvious_violation", "ambiguous");
    private static final int BATCH_SIZE = 20;
    private static final short SETTINGS_ID = 1;

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final ModerationSettingsRepository moderationSettingsRepository;
    private final OpenRouterReportClassifier classifier;

    public ReportClassificationService(
            ReportRepository reportRepository,
            CommentRepository commentRepository,
            ModerationSettingsRepository moderationSettingsRepository,
            OpenRouterReportClassifier classifier) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.moderationSettingsRepository = moderationSettingsRepository;
        this.classifier = classifier;
    }

    @Transactional
    public int classifyPending() {
        if (!classifier.isEnabled()) {
            return 0;
        }

        String model = moderationSettingsRepository.findById(SETTINGS_ID).orElseThrow().getClassificationModel();
        List<Long> candidateIds = reportRepository.findDistinctCommentIdsWithUnclassifiedPendingReports(
                PageRequest.of(0, BATCH_SIZE));
        List<Comment> candidates = commentRepository.findAllById(candidateIds);

        int classified = 0;
        for (Comment comment : candidates) {
            if (classifyOne(comment, model)) {
                classified++;
            }
        }
        return classified;
    }

    private boolean classifyOne(Comment comment, String model) {
        List<Report> reports = reportRepository.findByStatusAndComment_Id(PENDING, comment.getId());
        if (reports.isEmpty()) {
            return false;
        }

        List<String> reasons = reports.stream().map(Report::getReason).toList();
        List<String> details = reports.stream().map(Report::getDetail).filter(Objects::nonNull).toList();

        ReportClassification result;
        try {
            result = classifier.classify(model, comment.getBody(), reasons, details);
        } catch (ReportClassificationFailed e) {
            log.warn("신고 분류 실패, 건너뜀(commentId={}): {}", comment.getId(), e.getMessage());
            return false;
        }

        if (!VALID_VERDICTS.contains(result.verdict())) {
            return false;
        }

        comment.applyLlmClassification(result.verdict(), result.reason(), OffsetDateTime.now());
        return true;
    }
}
