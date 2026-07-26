package com.siso.backend.comment;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.ratelimit.RateLimiter;
import com.siso.backend.settings.ElectionSettings;
import com.siso.backend.settings.ElectionSettingsRepository;
import com.siso.backend.settings.ModerationSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportService {

    private static final Set<String> REASONS = Set.of("abuse", "hate", "spam", "etc");
    private static final String PENDING = "pending";
    private static final String REJECTED = "rejected";
    private static final String BLINDED = "blinded";
    private static final short SETTINGS_ID = 1;

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final ModerationSettingsRepository moderationSettingsRepository;
    private final ElectionSettingsRepository electionSettingsRepository;
    private final RateLimiter rateLimiter;

    public ReportService(
            ReportRepository reportRepository,
            CommentRepository commentRepository,
            AdminAlertRepository adminAlertRepository,
            ModerationSettingsRepository moderationSettingsRepository,
            ElectionSettingsRepository electionSettingsRepository,
            RateLimiter rateLimiter) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.adminAlertRepository = adminAlertRepository;
        this.moderationSettingsRepository = moderationSettingsRepository;
        this.electionSettingsRepository = electionSettingsRepository;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void create(Long commentId, UUID anonId, String reason, String detail) {
        if (!REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason must be abuse, hate, spam, or etc");
        }

        rateLimiter.checkOrThrow("report", anonId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));

        if (reportRepository.existsByComment_IdAndAnonId(commentId, anonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 신고한 댓글입니다");
        }

        reportRepository.save(new Report(comment, anonId, reason, detail, OffsetDateTime.now()));

        int threshold = moderationSettingsRepository.findById(SETTINGS_ID).orElseThrow().getAutoBlindReportThreshold();
        ElectionSettings electionSettings = electionSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        if (electionSettings.isEnabled()) {
            // D10: 선거 모드 중엔 어드민이 평소 설정한 임계값을 그대로 두고,
            // 그보다 낮을 때만(더 엄격해질 때만) override 값을 적용한다 —
            // 선거 모드가 실수로 임계값을 올려버리는 일이 없도록.
            threshold = Math.min(threshold, electionSettings.getOverrideAutoBlindThreshold());
        }
        // 관리자가 dismiss(rejected)한 신고는 이미 무근거로 판단된 것이므로
        // 임계값 계산에서 제외한다 — 안 그러면 기각된 신고가 나중에 들어온
        // 무관한 신고와 합산되어 되살아난다.
        long totalReports = reportRepository.countByComment_IdAndStatusNot(commentId, REJECTED);
        if (totalReports >= threshold && !BLINDED.equals(comment.getStatus())) {
            comment.blind();
            reportRepository.findByStatusAndComment_Id(PENDING, commentId).forEach(Report::accept);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("commentId", commentId);
            payload.put("pairId", comment.getPair() == null ? null : comment.getPair().getId());
            payload.put("reportCount", totalReports);
            adminAlertRepository.save(new AdminAlert("comment_auto_blinded", payload, OffsetDateTime.now()));
        }
    }
}
