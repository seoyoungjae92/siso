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
 *
 * classifyPending() 전체를 감싸는 트랜잭션은 의도적으로 없다 — 배치당
 * 최대 20번 순차적으로 도는 OpenRouter HTTP 호출 내내 DB 커넥션 하나를
 * 계속 붙잡아두던 문제(감사에서 지적, 현재 트래픽 규모에선 위험 낮다고
 * 확인됐지만 구조는 고쳐둠)를 없애기 위해, 댓글 하나당 결과를
 * commentRepository.save()로 그때그때 짧게 커밋한다.
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

    public int classifyPending() {
        if (!classifier.isEnabled()) {
            return 0;
        }

        String model = moderationSettingsRepository.findById(SETTINGS_ID).orElseThrow().getClassificationModel();
        List<Long> candidateIds = reportRepository.findDistinctCommentIdsWithUnclassifiedPendingReports(
                PageRequest.of(0, BATCH_SIZE));

        int classified = 0;
        for (Long commentId : candidateIds) {
            if (classifyOne(commentId, model)) {
                classified++;
            }
        }
        return classified;
    }

    private boolean classifyOne(Long commentId, String model) {
        List<Report> reports = reportRepository.findByStatusAndComment_Id(PENDING, commentId);
        if (reports.isEmpty()) {
            return false;
        }

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        List<String> reasons = reports.stream().map(Report::getReason).toList();
        List<String> details = reports.stream().map(Report::getDetail).filter(Objects::nonNull).toList();

        ReportClassification result;
        try {
            // OpenRouter 호출 동안은 DB 트랜잭션을 전혀 열어두지 않는다.
            result = classifier.classify(model, comment.getBody(), reasons, details);
        } catch (ReportClassificationFailed e) {
            log.warn("신고 분류 실패, 건너뜀(commentId={}): {}", commentId, e.getMessage());
            return false;
        }

        if (!VALID_VERDICTS.contains(result.verdict())) {
            return false;
        }

        comment.applyLlmClassification(result.verdict(), result.reason(), OffsetDateTime.now());
        commentRepository.save(comment);
        return true;
    }
}
