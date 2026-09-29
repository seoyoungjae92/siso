-- 주제 중복 억제 파라미터를 코호트 설정에서 분리.
--
-- 기존에는 (1) 코호트 결속 임계값을 중복 판정에도 재사용하고 (2) 조회 창이
-- 24시간 고정이었다. 그래서 생성량을 줄이려고 코호트 임계값을 0.65→0.70으로
-- 올렸더니 중복 판정도 같이 빡빡해졌고, 노출 기간(7일)보다 창이 짧아
-- 며칠에 걸쳐 이어지는 이슈는 매번 새 주제가 됐다(2026-09-29 실측: 화면에
-- 보이는 15개 중 6개가 "DMZ 지뢰 폭발 사고").
--
-- 기본값 근거(2026-09-29 실측): 같은 이슈 주제쌍의 같은 편 글 최대 유사도는
-- 중앙값 0.82, 서로 다른 주제는 0.773이 최대 — 그 사이인 0.78로 잡는다.
ALTER TABLE crawl_settings
    ADD COLUMN dedup_similarity_threshold REAL NOT NULL DEFAULT 0.78
        CHECK (dedup_similarity_threshold BETWEEN 0 AND 1),
    ADD COLUMN dedup_lookback_hours INT NOT NULL DEFAULT 168
        CHECK (dedup_lookback_hours BETWEEN 1 AND 720);
