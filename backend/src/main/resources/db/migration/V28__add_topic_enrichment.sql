-- 토론 페이지 보강 필드(쟁점 배경 / 좌우 핵심 논거 / 생각해볼 질문).
-- 애드센스 "가치가 별로 없는 콘텐츠" 반려 대응으로 합성 단계에서 함께
-- 생성한다. 보강이 없는 주제(이 마이그레이션 이전 주제 포함)는 네 컬럼이
-- 모두 NULL이고, 프론트는 background IS NULL이면 검색 색인에서 제외한다.
ALTER TABLE topic_pairs
    ADD COLUMN background TEXT,
    ADD COLUMN left_points TEXT[],
    ADD COLUMN right_points TEXT[],
    ADD COLUMN discussion_questions TEXT[];
