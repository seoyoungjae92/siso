package com.siso.backend.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f "
            + "WHERE (:category IS NULL OR f.category = :category) AND (:status IS NULL OR f.status = :status) "
            + "ORDER BY f.createdAt DESC")
    List<Feedback> findByFilters(@Param("category") String category, @Param("status") String status, Pageable pageable);

    @Modifying
    @Query("UPDATE Feedback f SET f.contact = NULL WHERE f.createdAt < :cutoff AND f.contact IS NOT NULL")
    int purgeContactOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
