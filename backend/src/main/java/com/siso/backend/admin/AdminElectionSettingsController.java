package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/election-settings")
public class AdminElectionSettingsController {

    private final AdminElectionSettingsService adminElectionSettingsService;

    public AdminElectionSettingsController(AdminElectionSettingsService adminElectionSettingsService) {
        this.adminElectionSettingsService = adminElectionSettingsService;
    }

    @GetMapping
    public ElectionSettingsDto get() {
        return adminElectionSettingsService.get();
    }

    @PutMapping
    public ElectionSettingsDto update(@RequestBody ElectionSettingsRequest request) {
        return adminElectionSettingsService.update(request);
    }
}
