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
}
