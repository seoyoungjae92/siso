package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation-settings")
public class AdminModerationSettingsController {

    private final AdminModerationSettingsService adminModerationSettingsService;

    public AdminModerationSettingsController(AdminModerationSettingsService adminModerationSettingsService) {
        this.adminModerationSettingsService = adminModerationSettingsService;
    }

    @GetMapping
    public ModerationSettingsDto get() {
        return adminModerationSettingsService.get();
    }

    @PutMapping
    public ModerationSettingsDto update(@RequestBody ModerationSettingsRequest request) {
        return adminModerationSettingsService.update(request);
    }
}
