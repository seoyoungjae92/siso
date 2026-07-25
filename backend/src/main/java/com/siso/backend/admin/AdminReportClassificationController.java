package com.siso.backend.admin;

import com.siso.backend.moderation.ReportClassificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/report-classification")
public class AdminReportClassificationController {

    private final ReportClassificationService reportClassificationService;

    public AdminReportClassificationController(ReportClassificationService reportClassificationService) {
        this.reportClassificationService = reportClassificationService;
    }

    @PostMapping("/run")
    public int runNow() {
        return reportClassificationService.classifyPending();
    }
}
