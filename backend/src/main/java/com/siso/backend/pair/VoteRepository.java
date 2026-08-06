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

    // 목록 화면(페이지당 N개 쌍)에 투표 요약을 보여줄 때 쌍마다 위 쿼리를
    // 따로 부르면 N+1이 되므로, 페이지에 있는 쌍 id를 한 번에 묶어서
    // 조회한다 — pairId까지 같이 뽑아서 호출부에서 쌍별로 다시 묶는다.
    @Query("SELECT v.pair.id AS pairId, v.stance AS stance, SUM(au.trustScore) AS total FROM Vote v, AnonUser au "
            + "WHERE v.pair.id IN :pairIds AND v.anonId = au.anonId GROUP BY v.pair.id, v.stance")
    List<WeightedStanceCountByPair> sumWeightedByPairIdsGroupByStance(@Param("pairIds") List<Long> pairIds);

    // "N명 투표" 표시용 — 신뢰도 가중치 합(sumWeighted*)은 퍼센트 막대 계산
    // 전용이라 실제 투표자 수와 다르다(예: 투표자 2명인데 가중치 합이
    // 0.89라 반올림하면 "1명"으로 잘못 보임, 2026-08-06 실측). 화면에
    // 보여줄 인원수는 항상 이 단순 COUNT를 써야 한다 — comment 쪽과 동일한
    // 배치 패턴, 단일 조회도 List.of(id)로 통일해서 쿼리 하나로 관리한다.
    @Query("SELECT v.pair.id AS pairId, COUNT(v) AS total FROM Vote v WHERE v.pair.id IN :pairIds GROUP BY v.pair.id")
    List<VoteCountByPair> countByPairIds(@Param("pairIds") List<Long> pairIds);

    interface WeightedStanceCount {
        String getStance();

        double getTotal();
    }

    interface WeightedStanceCountByPair {
        Long getPairId();

        String getStance();

        double getTotal();
    }

    interface VoteCountByPair {
        Long getPairId();

        long getTotal();
    }
}
