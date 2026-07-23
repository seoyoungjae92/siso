package com.siso.backend.post;

import com.siso.backend.source.Side;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 화면에 보이는 "N시간 전" 표시가 publishedAt(있으면)을 우선 쓰므로,
    // 정렬도 같은 기준(COALESCE)으로 맞춰야 표시 순서와 표시 시각이
    // 어긋나지 않는다 — collectedAt만으로 정렬하면 오늘의유머처럼
    // publishedAt이 없는 소스와 섞였을 때 순서가 실제 게시 시각과
    // 안 맞을 수 있음.
    @Query("""
            SELECT p FROM Post p
            WHERE p.source.side = :side
              AND p.source.enabled = true
              AND p.collectedAt > :since
            ORDER BY COALESCE(p.publishedAt, p.collectedAt) DESC
            """)
    Page<Post> findFeed(@Param("side") Side side, @Param("since") OffsetDateTime since, Pageable pageable);

    long countBySource_Id(Long sourceId);

    Optional<Post> findFirstBySource_IdOrderByCollectedAtDesc(Long sourceId);
}
