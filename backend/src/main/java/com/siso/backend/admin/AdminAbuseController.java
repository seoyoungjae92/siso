package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/abuse-alerts")
public class AdminAbuseController {

    private final AdminAbuseService adminAbuseService;

    public AdminAbuseController(AdminAbuseService adminAbuseService) {
        this.adminAbuseService = adminAbuseService;
    }

    @GetMapping
    public List<AdminAlertDto> getAlerts(@RequestParam(required = false) Boolean resolved) {
        return adminAbuseService.getAlerts(resolved);
    }

    @PostMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        adminAbuseService.resolve(id);
    }

    @GetMapping("/ip-clusters")
    public List<IpClusterDto> getIpClusters() {
        return adminAbuseService.getIpClusters();
    }
}
