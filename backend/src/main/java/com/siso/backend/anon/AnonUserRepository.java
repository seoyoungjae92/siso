package com.siso.backend.anon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnonUserRepository extends JpaRepository<AnonUser, UUID> {
}
