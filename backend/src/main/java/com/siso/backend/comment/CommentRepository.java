package com.siso.backend.comment;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPair_IdAndStatusNot(Long pairId, String excludedStatus, Sort sort);
}
