package com.siso.backend.pair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByPair_IdAndAnonId(Long pairId, UUID anonId);

    // 퍼센트 막대는 신뢰도 가중치 없이 순수 투표수 비율로 표시한다
    // (2026-08-12 결정) — 투표자가 2명뿐인데 신뢰도 차이 때문에 66:34처럼
    // 직관과 안 맞는 비율이 나와 혼란을 줬음. 어뷰징 탐지 자체(신뢰도 점수
    // 계산·다중계정 클러스터 알림, TrustScoreService)는 그대로 유지하고,
    // 화면 표시에만 안 쓰는 것으로 범위를 좁혔다.
    @Query("SELECT v.stance AS stance, COUNT(v) AS total FROM Vote v WHERE v.pair.id = :pairId GROUP BY v.stance")
    List<StanceCount> countByPairIdGroupByStance(@Param("pairId") Long pairId);

    // 목록 화면(페이지당 N개 쌍)에 투표 요약을 보여줄 때 쌍마다 위 쿼리를
    // 따로 부르면 N+1이 되므로, 페이지에 있는 쌍 id를 한 번에 묶어서
    // 조회한다. "N명 투표" 표시용 총 투표수도 이 결과를 진영별로 합산해서
    // 만든다 — 예전엔 별도 COUNT 쿼리가 하나 더 있었는데, 가중치를 없앤
    // 김에 합쳐서 왕복을 하나 줄였다(Railway↔Supabase 리전 간 지연이 커서
    // 요청당 DB 왕복 수 자체가 체감 속도에 직접 영향, 2026-08-12).
    @Query("SELECT v.pair.id AS pairId, v.stance AS stance, COUNT(v) AS total FROM Vote v "
            + "WHERE v.pair.id IN :pairIds GROUP BY v.pair.id, v.stance")
    List<StanceCountByPair> countByPairIdsGroupByStance(@Param("pairIds") List<Long> pairIds);

    interface StanceCount {
        String getStance();

        double getTotal();
    }

    interface StanceCountByPair {
        Long getPairId();

        String getStance();

        double getTotal();
    }
}
