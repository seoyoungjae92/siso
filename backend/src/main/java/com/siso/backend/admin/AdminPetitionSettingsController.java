package com.siso.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/petition-settings")
public class AdminPetitionSettingsController {

    private final AdminPetitionSettingsService adminPetitionSettingsService;

    public AdminPetitionSettingsController(AdminPetitionSettingsService adminPetitionSettingsService) {
        this.adminPetitionSettingsService = adminPetitionSettingsService;
    }

    @GetMapping
    public PetitionSettingsDto get() {
        return adminPetitionSettingsService.get();
    }

    @PutMapping
    public PetitionSettingsDto update(@RequestBody PetitionSettingsRequest request) {
        return adminPetitionSettingsService.update(request);
    }
}
