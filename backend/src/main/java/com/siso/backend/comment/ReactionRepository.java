package com.siso.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByComment_IdAndAnonId(Long commentId, UUID anonId);

    List<Reaction> findByComment_IdInAndAnonId(List<Long> commentIds, UUID anonId);
}
