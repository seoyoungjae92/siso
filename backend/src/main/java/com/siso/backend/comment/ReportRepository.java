package com.siso.backend.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByComment_IdAndAnonId(Long commentId, UUID anonId);

    List<Report> findByStatusOrderByCreatedAtAsc(String status);

    List<Report> findByStatusAndComment_Id(String status, Long commentId);

    long countByStatus(String status);

    long countByComment_Id(Long commentId);

    @Query("SELECT DISTINCT r.comment FROM Report r "
            + "WHERE r.status = 'pending' AND r.comment.llmVerdict IS NULL "
            + "ORDER BY r.comment.id")
    List<Comment> findDistinctCommentsWithUnclassifiedPendingReports(Pageable pageable);
}
