package com.siso.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByComment_IdAndAnonId(Long commentId, UUID anonId);

    List<Report> findByStatusOrderByCreatedAtAsc(String status);

    List<Report> findByStatusAndComment_Id(String status, Long commentId);

    long countByStatus(String status);

    long countByComment_Id(Long commentId);
}
