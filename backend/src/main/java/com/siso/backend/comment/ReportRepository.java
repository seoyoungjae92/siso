package com.siso.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByComment_IdAndAnonId(Long commentId, UUID anonId);
}
