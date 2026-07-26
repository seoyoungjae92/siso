package com.siso.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * D10 — 선거 모드. 켜지면(enabled) 사이트 전체 투표 위젯/비율 그래프를
 * 숨기고(공직선거법상 선거일 전 여론조사 공표 금지 리스크 회피), 신고
 * 자동 블라인드 임계값을 overrideAutoBlindThreshold로 임시 하향한다 —
 * 어드민이 평소 설정한 값(ModerationSettings)은 그대로 두고, 켜져 있는
 * 동안만 더 낮은 값을 적용한다(꺼지면 원래 설정으로 자동 복귀).
 */
@Entity
@Table(name = "election_settings")
public class ElectionSettings {

    @Id
    private Short id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "override_auto_blind_threshold", nullable = false)
    private int overrideAutoBlindThreshold;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ElectionSettings() {
    }

    public Short getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOverrideAutoBlindThreshold() {
        return overrideAutoBlindThreshold;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(boolean enabled, int overrideAutoBlindThreshold, OffsetDateTime updatedAt) {
        this.enabled = enabled;
        this.overrideAutoBlindThreshold = overrideAutoBlindThreshold;
        this.updatedAt = updatedAt;
    }
}
