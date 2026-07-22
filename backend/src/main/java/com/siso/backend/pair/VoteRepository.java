package com.siso.backend.pair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByPair_IdAndAnonId(Long pairId, UUID anonId);

    @Query("SELECT v.stance AS stance, COUNT(v) AS total FROM Vote v WHERE v.pair.id = :pairId GROUP BY v.stance")
    List<StanceCount> countByPairIdGroupByStance(@Param("pairId") Long pairId);

    interface StanceCount {
        String getStance();

        long getTotal();
    }
}
