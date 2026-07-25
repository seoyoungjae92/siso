package com.siso.backend.admin;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminPetitionSettingsService {

    private static final short SINGLETON_ID = 1;

    private final PetitionSettingsRepository petitionSettingsRepository;

    public AdminPetitionSettingsService(PetitionSettingsRepository petitionSettingsRepository) {
        this.petitionSettingsRepository = petitionSettingsRepository;
    }

    @Transactional(readOnly = true)
    public PetitionSettingsDto get() {
        return toDto(findSingleton());
    }

    @Transactional
    public PetitionSettingsDto update(PetitionSettingsRequest request) {
        PetitionSettings settings = findSingleton();
        settings.update(request.eraco(), request.topN(), request.windowDays(), OffsetDateTime.now());
        return toDto(settings);
    }

    private PetitionSettings findSingleton() {
        return petitionSettingsRepository.findById(SINGLETON_ID).orElseThrow();
    }

    private PetitionSettingsDto toDto(PetitionSettings settings) {
        return new PetitionSettingsDto(
                settings.getEraco(), settings.getTopN(), settings.getWindowDays(), settings.getUpdatedAt());
    }
}
