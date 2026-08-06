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

    Optional<TopicPair> findByIdAndStatusAndTitleIsNotNull(Long id, String status);

    // 메인 피드도 "오늘의 링"과 동일하게 참여도(투표+댓글) 합이 많은 순으로
    // 배치한다 — 동점이면 반응(투표)보다 댓글이 많은 쪽을 우선하고, 그마저
    // 같으면 최신순(findTopEngagementSince와 동일한 이유로 필요 — 0/0인
    // 주제끼리 순서가 매 조회마다 임의로 바뀌는 것 방지).
    @Query(value = "SELECT p FROM TopicPair p WHERE p.status = :status AND p.title IS NOT NULL AND p.createdAt >= :since "
            + "ORDER BY (SELECT COUNT(v) FROM Vote v WHERE v.pair = p) "
            + "+ (SELECT COUNT(c) FROM Comment c WHERE c.pair = p AND c.status = 'visible') DESC, "
            + "(SELECT COUNT(c) FROM Comment c WHERE c.pair = p AND c.status = 'visible') DESC, "
            + "p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM TopicPair p WHERE p.status = :status AND p.title IS NOT NULL AND p.createdAt >= :since")
    Page<TopicPair> findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
            @Param("status") String status, @Param("since") OffsetDateTime since, Pageable pageable);

    // 뉴스레터 주간 리포트/오늘의 링 선정용 — 신뢰도 가중치까지는 필요 없는
    // 단순 랭킹이라(VoteRepository의 가중합 대신) 투표+댓글 단순 카운트
    // 합으로 정렬한다. 참여도가 0인 주제끼리는 동점이라 최신순을 2차
    // 정렬로 둔다 — 안 그러면 실 데이터(대부분 0/0)에서 순서가 매 조회마다
    // 임의로 바뀌어 오늘의 링이 매번 다른 글로 튐(2026-08-02 실측).
    @Query("SELECT p FROM TopicPair p WHERE p.status = 'active' AND p.title IS NOT NULL AND p.createdAt >= :since "
            + "ORDER BY (SELECT COUNT(v) FROM Vote v WHERE v.pair = p) "
            + "+ (SELECT COUNT(c) FROM Comment c WHERE c.pair = p AND c.status = 'visible') DESC, "
            + "p.createdAt DESC")
    List<TopicPair> findTopEngagementSince(@Param("since") OffsetDateTime since, Pageable pageable);
}
