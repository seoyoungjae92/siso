package com.siso.backend.admin;

import com.siso.backend.retention.RetentionPurgeResult;
import com.siso.backend.retention.RetentionPurgeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/retention")
public class AdminRetentionController {

    private final RetentionPurgeService retentionPurgeService;

    public AdminRetentionController(RetentionPurgeService retentionPurgeService) {
        this.retentionPurgeService = retentionPurgeService;
    }

    @PostMapping("/purge")
    public RetentionPurgeResult purgeNow() {
        return retentionPurgeService.purge();
    }
}
