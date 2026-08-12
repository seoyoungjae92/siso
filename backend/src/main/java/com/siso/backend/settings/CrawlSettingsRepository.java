package com.siso.backend.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrawlSettingsRepository extends JpaRepository<CrawlSettings, Short> {

    // PairService.getPairs/getFeaturedPair가 매 요청마다 crawl_settings.
    // display_window_days와 election_settings.enabled를 따로 조회하던 걸
    // 하나로 묶은 것 — Railway(도쿄)↔Supabase(뭄바이) 리전 간 지연이 커서
    // 요청당 DB 왕복 수 자체가 체감 속도에 직접 영향을 준다(2026-08-12).
    // 두 설정 다 단일행(id=1) 테이블이라 안전하게 크로스 조인 가능. 원래
    // 소속 아닌 election_settings까지 여기서 조회하는 예외적인 쿼리이니
    // 다른 곳에서 안 따라 하고 늘리지 말 것.
    @Query(
            value = "SELECT cs.display_window_days AS displayWindowDays, es.enabled AS electionEnabled "
                    + "FROM crawl_settings cs, election_settings es WHERE cs.id = 1 AND es.id = 1",
            nativeQuery = true)
    DisplayWindowAndElectionMode findDisplayWindowAndElectionMode();

    interface DisplayWindowAndElectionMode {
        int getDisplayWindowDays();

        boolean getElectionEnabled();
    }
}
