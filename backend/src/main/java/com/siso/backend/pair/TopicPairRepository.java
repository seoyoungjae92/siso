package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TopicPairRepository extends JpaRepository<TopicPair, Long> {

    Page<TopicPair> findByStatusAndTitleIsNotNullAndCreatedAtAfter(
            String status, OffsetDateTime since, Pageable pageable);

    Optional<TopicPair> findByIdAndStatusAndTitleIsNotNull(Long id, String status);

    // 뉴스레터 주간 리포트용 — 신뢰도 가중치까지는 필요 없는 단순 랭킹이라
    // (VoteRepository의 가중합 대신) 투표+댓글 단순 카운트 합으로 정렬한다.
    @Query("SELECT p FROM TopicPair p WHERE p.status = 'active' AND p.title IS NOT NULL AND p.createdAt >= :since "
            + "ORDER BY (SELECT COUNT(v) FROM Vote v WHERE v.pair = p) "
            + "+ (SELECT COUNT(c) FROM Comment c WHERE c.pair = p AND c.status = 'visible') DESC")
    List<TopicPair> findTopEngagementSince(@Param("since") OffsetDateTime since, Pageable pageable);
}
