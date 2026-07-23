package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/crawl-settings")
public class AdminCrawlSettingsController {

    private final AdminCrawlSettingsService adminCrawlSettingsService;

    public AdminCrawlSettingsController(AdminCrawlSettingsService adminCrawlSettingsService) {
        this.adminCrawlSettingsService = adminCrawlSettingsService;
    }

    @GetMapping
    public CrawlSettingsDto get() {
        return adminCrawlSettingsService.get();
    }

    @PutMapping
    public CrawlSettingsDto update(@RequestBody CrawlSettingsRequest request) {
        return adminCrawlSettingsService.update(request);
    }
}
