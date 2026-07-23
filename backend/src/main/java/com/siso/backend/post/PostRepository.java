package com.siso.backend.post;

import com.siso.backend.source.Side;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findBySource_SideAndSource_EnabledTrueAndCollectedAtAfter(
            Side side, OffsetDateTime since, Pageable pageable);

    long countBySource_Id(Long sourceId);

    Optional<Post> findFirstBySource_IdOrderByCollectedAtDesc(Long sourceId);
}
