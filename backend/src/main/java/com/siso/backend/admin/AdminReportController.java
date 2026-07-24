package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public List<PendingReportGroupDto> getPending() {
        return adminReportService.getPendingGroupedByComment();
    }

    @PostMapping("/{commentId}/moderate")
    public void moderate(@PathVariable Long commentId, @RequestBody ModerationRequest request) {
        adminReportService.moderate(commentId, request.action());
    }

    @GetMapping("/history")
    public List<BlindHistoryDto> getHistory() {
        return adminReportService.getBlindHistory();
    }
}
