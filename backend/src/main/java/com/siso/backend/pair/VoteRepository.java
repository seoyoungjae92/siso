package com.siso.backend.pair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByPair_IdAndAnonId(Long pairId, UUID anonId);

    // Vote.anonId/AnonUser.anonId 둘 다 UUID 컬럼일 뿐 JPA 연관관계가
    // 없어서(설계상 별개 애그리게잇) theta-join으로 직접 이어준다 —
    // 신뢰도 가중치(6절)를 반영한 집계라 COUNT 대신 SUM(trustScore).
    @Query("SELECT v.stance AS stance, SUM(au.trustScore) AS total FROM Vote v, AnonUser au "
            + "WHERE v.pair.id = :pairId AND v.anonId = au.anonId GROUP BY v.stance")
    List<WeightedStanceCount> sumWeightedByPairIdGroupByStance(@Param("pairId") Long pairId);

    interface WeightedStanceCount {
        String getStance();

        double getTotal();
    }
}
