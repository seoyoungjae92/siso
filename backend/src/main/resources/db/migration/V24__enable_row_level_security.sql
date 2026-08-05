-- Supabase는 프로젝트의 모든 public 스키마 테이블을 PostgREST API로
-- 자동 노출한다(우리가 그 API를 쓰든 안 쓰든 상관없이 켜져있음). 우리
-- 백엔드는 직접 DB 커넥션(테이블 소유자 권한)으로만 접속하고 PostgREST는
-- 전혀 안 쓰므로, RLS를 켜고 정책을 하나도 안 만드는 것 자체가 올바른
-- 설정이다 — anon/authenticated 롤(PostgREST 경유 요청)은 완전히 차단되고,
-- 테이블 소유자 권한으로 접속하는 우리 백엔드/크롤러/Flyway는 영향받지
-- 않는다(Postgres 기본 동작: 테이블 소유자는 RLS를 우회함).
-- Supabase 보안 어드바이저 경고(rls_disabled_in_public, 2026-08-04) 대응.

ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE topic_pairs ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE anon_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE election_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE abuse_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE petition_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE moderation_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE petitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE newsletter_subscribers ENABLE ROW LEVEL SECURITY;
ALTER TABLE crawl_settings ENABLE ROW LEVEL SECURITY;
