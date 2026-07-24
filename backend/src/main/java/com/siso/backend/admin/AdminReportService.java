package com.siso.backend.admin;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.comment.Comment;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.Report;
import com.siso.backend.comment.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    private static final String PENDING = "pending";

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AdminAlertRepository adminAlertRepository;

    public AdminReportService(
            ReportRepository reportRepository,
            CommentRepository commentRepository,
            AdminAlertRepository adminAlertRepository) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.adminAlertRepository = adminAlertRepository;
    }

    @Transactional(readOnly = true)
    public List<PendingReportGroupDto> getPendingGroupedByComment() {
        List<Report> pending = reportRepository.findByStatusOrderByCreatedAtAsc(PENDING);

        Map<Long, List<Report>> byComment = pending.stream()
                .collect(Collectors.groupingBy(
                        report -> report.getComment().getId(), LinkedHashMap::new, Collectors.toList()));

        return byComment.values().stream()
                .map(this::toGroupDto)
                .toList();
    }

    private PendingReportGroupDto toGroupDto(List<Report> reports) {
        Comment comment = reports.get(0).getComment();
        Map<String, Long> reasonCounts = reports.stream()
                .collect(Collectors.groupingBy(Report::getReason, LinkedHashMap::new, Collectors.counting()));
        OffsetDateTime oldest = reports.stream()
                .map(Report::getCreatedAt)
                .min(OffsetDateTime::compareTo)
                .orElseThrow();

        return new PendingReportGroupDto(
                comment.getId(),
                comment.getBody(),
                comment.getNickname(),
                comment.getPair() == null ? null : comment.getPair().getId(),
                reasonCounts,
                reports.size(),
                oldest);
    }

    @Transactional
    public void moderate(Long commentId, String action) {
        List<Report> reports = reportRepository.findByStatusAndComment_Id(PENDING, commentId);
        if (reports.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no pending reports for this comment");
        }

        if ("blind".equals(action)) {
            Comment comment = reports.get(0).getComment();
            comment.blind();
            reports.forEach(Report::accept);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("commentId", commentId);
            payload.put("pairId", comment.getPair() == null ? null : comment.getPair().getId());
            payload.put("reportCount", reports.size());
            adminAlertRepository.save(new AdminAlert("comment_manually_blinded", payload, OffsetDateTime.now()));
        } else if ("dismiss".equals(action)) {
            reports.forEach(Report::reject);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be 'blind' or 'dismiss'");
        }
    }

    @Transactional(readOnly = true)
    public List<BlindHistoryDto> getBlindHistory() {
        return adminAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private BlindHistoryDto toHistoryDto(AdminAlert alert) {
        Long commentId = asLong(alert.getPayload().get("commentId"));
        Comment comment = commentId == null ? null : commentRepository.findById(commentId).orElse(null);
        Integer reportCount = asInteger(alert.getPayload().get("reportCount"));

        return new BlindHistoryDto(
                alert.getId(),
                alert.getType(),
                commentId,
                comment == null ? null : comment.getBody(),
                comment == null ? null : comment.getNickname(),
                comment == null ? null : (comment.getPair() == null ? null : comment.getPair().getId()),
                reportCount,
                alert.getCreatedAt());
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
