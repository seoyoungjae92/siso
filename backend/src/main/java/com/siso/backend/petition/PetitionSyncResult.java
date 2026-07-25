package com.siso.backend.petition;

import java.time.OffsetDateTime;

public record PetitionSyncResult(int upserted, int closed, OffsetDateTime syncedAt) {
}
