package com.siso.backend.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 목록 화면(페이지당 N개 쌍)에 댓글 수를 보여줄 때 쌍마다 따로 부르면
    // N+1이 되므로 한 번에 묶어서 조회한다 — vote 쪽과 동일한 배치 패턴.
    // blinded/deleted는 실제로 보이는 토론이 아니므로 visible만 센다.
    @Query("SELECT c.pair.id AS pairId, COUNT(c) AS total FROM Comment c "
            + "WHERE c.pair.id IN :pairIds AND c.status = 'visible' GROUP BY c.pair.id")
    List<CommentCountByPair> countVisibleByPairIds(@Param("pairIds") List<Long> pairIds);

    interface CommentCountByPair {
        Long getPairId();

        long getTotal();
    }

    // 7절: IP 해시는 수집일(작성일)로부터 90일 후 파기 — 댓글 본문/작성자
    // 정보는 그대로 두고 ip_hash만 지운다(현재 코드베이스에서 이 값을
    // 읽는 곳이 없음 — 어뷰징 탐지는 anon_users.ip_hash_recent를 씀).
    @Modifying
    @Query("UPDATE Comment c SET c.ipHash = NULL WHERE c.createdAt < :cutoff AND c.ipHash IS NOT NULL")
    int purgeIpHashOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
