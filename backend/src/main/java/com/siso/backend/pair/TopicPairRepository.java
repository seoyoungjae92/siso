package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicPairRepository extends JpaRepository<TopicPair, Long> {

    Page<TopicPair> findByStatus(String status, Pageable pageable);
}
