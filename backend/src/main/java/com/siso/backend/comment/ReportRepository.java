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

    long countByComment_IdAndStatusNot(Long commentId, String status);

    // PostgreSQL은 SELECT DISTINCT의 ORDER BY 표현식이 SELECT 목록에 그대로
    // 있어야 해서(실측으로 확인 — 엔티티 전체를 DISTINCT+ORDER BY하면 에러남),
    // 엔티티가 아니라 id만 distinct로 뽑아 호출부에서 CommentRepository로
    // 다시 조회한다.
    @Query("SELECT DISTINCT r.comment.id FROM Report r "
            + "WHERE r.status = 'pending' AND r.comment.llmVerdict IS NULL "
            + "ORDER BY r.comment.id")
    List<Long> findDistinctCommentIdsWithUnclassifiedPendingReports(Pageable pageable);
}
