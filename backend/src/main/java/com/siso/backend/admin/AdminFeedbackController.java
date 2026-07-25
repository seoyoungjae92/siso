package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;

    public AdminFeedbackController(AdminFeedbackService adminFeedbackService) {
        this.adminFeedbackService = adminFeedbackService;
    }

    @GetMapping
    public List<AdminFeedbackDto> list(
            @RequestParam(required = false) String category, @RequestParam(required = false) String status) {
        return adminFeedbackService.list(category, status);
    }

    @PostMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        adminFeedbackService.resolve(id);
    }
}
