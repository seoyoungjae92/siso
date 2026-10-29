-- 같은 크론(좌 수집, 우 수집, 후처리)이 겹쳐 도는 걸 막는 임대(lease) 잠금.
--
-- advisory lock은 커넥션 단위라 Supabase 풀러가 트랜잭션 모드면 잠금을 잡은
-- 서버 커넥션이 풀로 돌아가 엉뚱하게 남을 수 있다. 행 하나에 만료 시각을 두면
-- 풀러 모드와 상관없이 동작하고, 크롤러가 죽어도 만료 뒤에는 저절로 풀린다.
CREATE TABLE crawler_run_lock (
    name         TEXT PRIMARY KEY,
    locked_until TIMESTAMPTZ NOT NULL
);

-- V24와 같은 이유: PostgREST로 노출되지 않게 RLS만 켜고 정책은 두지 않는다.
ALTER TABLE crawler_run_lock ENABLE ROW LEVEL SECURITY;
