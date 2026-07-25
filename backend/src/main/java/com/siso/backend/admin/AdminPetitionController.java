package com.siso.backend.admin;

import com.siso.backend.petition.PetitionSyncResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/petitions")
public class AdminPetitionController {

    private final AdminPetitionService adminPetitionService;

    public AdminPetitionController(AdminPetitionService adminPetitionService) {
        this.adminPetitionService = adminPetitionService;
    }

    @GetMapping
    public List<AdminPetitionDto> list() {
        return adminPetitionService.list();
    }

    @PostMapping("/sync")
    public PetitionSyncResult syncNow() {
        return adminPetitionService.syncNow();
    }
}
