-- 하루 주제 생성 상한. 임계값(코호트 유사도/최소 글 개수)만으로는 뉴스
-- 상황에 따라 생성량이 크게 출렁여서, 목표치(하루 1~2건)를 결정적으로
-- 보장하지 못함(2026-09-21: 0.65/6 설정에서 하루 8건). 크롤러가 합성 전에
-- "오늘(KST) 이미 만들어진 주제 수"를 세어 이 값을 넘지 않게 한다.
ALTER TABLE crawl_settings
    ADD COLUMN max_topics_per_day INT NOT NULL DEFAULT 2
        CHECK (max_topics_per_day BETWEEN 1 AND 100);
