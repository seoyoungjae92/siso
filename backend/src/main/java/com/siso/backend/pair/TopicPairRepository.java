package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TopicPairRepository extends JpaRepository<TopicPair, Long> {

    Page<TopicPair> findByStatusAndTitleIsNotNullAndCreatedAtAfter(
            String status, OffsetDateTime since, Pageable pageable);

    Optional<TopicPair> findByIdAndStatusAndTitleIsNotNull(Long id, String status);
}
