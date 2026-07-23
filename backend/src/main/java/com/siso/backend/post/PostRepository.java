package com.siso.backend.post;

import com.siso.backend.source.Side;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findBySource_SideAndSource_EnabledTrue(Side side, Pageable pageable);

    long countBySource_Id(Long sourceId);

    Optional<Post> findFirstBySource_IdOrderByCollectedAtDesc(Long sourceId);
}
