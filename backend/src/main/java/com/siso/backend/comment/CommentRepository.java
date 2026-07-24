package com.siso.backend.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPair_IdAndStatusNot(Long pairId, String excludedStatus, Sort sort);

    @Query("SELECT c.body FROM Comment c WHERE c.anonId = :anonId AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<String> findRecentBodiesByAnonId(
            @Param("anonId") UUID anonId, @Param("since") OffsetDateTime since, Pageable pageable);
}
