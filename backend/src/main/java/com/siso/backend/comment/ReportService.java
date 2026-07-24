package com.siso.backend.comment;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
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
    private static final String BLINDED = "blinded";

    // 일단 20건으로 고정(사용자 확정) — 나중에 운영하면서 튜닝 필요해지면
    // crawl_settings처럼 어드민 설정으로 옮길 수 있음.
    private static final long AUTO_BLIND_THRESHOLD = 20;

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AdminAlertRepository adminAlertRepository;

    public ReportService(
            ReportRepository reportRepository,
            CommentRepository commentRepository,
            AdminAlertRepository adminAlertRepository) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.adminAlertRepository = adminAlertRepository;
    }

    @Transactional
    public void create(Long commentId, UUID anonId, String reason, String detail) {
        if (!REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason must be abuse, hate, spam, or etc");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));

        if (reportRepository.existsByComment_IdAndAnonId(commentId, anonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 신고한 댓글입니다");
        }

        reportRepository.save(new Report(comment, anonId, reason, detail, OffsetDateTime.now()));

        long totalReports = reportRepository.countByComment_Id(commentId);
        if (totalReports >= AUTO_BLIND_THRESHOLD && !BLINDED.equals(comment.getStatus())) {
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
