package com.siso.backend.admin;

import com.siso.backend.comment.Comment;
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

    public AdminReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
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
            reports.get(0).getComment().blind();
            reports.forEach(Report::accept);
        } else if ("dismiss".equals(action)) {
            reports.forEach(Report::reject);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be 'blind' or 'dismiss'");
        }
    }
}
