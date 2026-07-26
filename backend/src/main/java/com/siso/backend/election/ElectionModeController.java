package com.siso.backend.election;

import com.siso.backend.settings.ElectionSettings;
import com.siso.backend.settings.ElectionSettingsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D10 — 선거 모드 여부를 프론트가 확인하는 공개(비인증) 엔드포인트.
 * 켜져 있으면 프론트는 투표 위젯/비율 그래프를 숨긴다.
 */
@RestController
public class ElectionModeController {

    private static final short SETTINGS_ID = 1;

    private final ElectionSettingsRepository electionSettingsRepository;

    public ElectionModeController(ElectionSettingsRepository electionSettingsRepository) {
        this.electionSettingsRepository = electionSettingsRepository;
    }

    @GetMapping("/api/election-mode")
    public ElectionModeDto get() {
        boolean enabled =
                electionSettingsRepository.findById(SETTINGS_ID).map(ElectionSettings::isEnabled).orElse(false);
        return new ElectionModeDto(enabled);
    }
}
