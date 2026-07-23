package com.siso.backend.anon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AnonUserRepository extends JpaRepository<AnonUser, UUID> {

    long countByFirstSeenAfter(OffsetDateTime since);

    long countByLastSeenAfter(OffsetDateTime since);
}
