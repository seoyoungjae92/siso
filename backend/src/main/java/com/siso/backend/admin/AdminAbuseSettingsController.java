package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/abuse-settings")
public class AdminAbuseSettingsController {

    private final AdminAbuseSettingsService adminAbuseSettingsService;

    public AdminAbuseSettingsController(AdminAbuseSettingsService adminAbuseSettingsService) {
        this.adminAbuseSettingsService = adminAbuseSettingsService;
    }

    @GetMapping
    public AbuseSettingsDto get() {
        return adminAbuseSettingsService.get();
    }

    @PutMapping
    public AbuseSettingsDto update(@RequestBody AbuseSettingsRequest request) {
        return adminAbuseSettingsService.update(request);
    }
}
