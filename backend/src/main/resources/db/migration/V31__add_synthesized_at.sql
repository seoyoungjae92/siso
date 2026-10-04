-- 하루 주제 생성 상한(max_topics_per_day)을 "합성 시각" 기준으로 세기 위한 컬럼.
--
-- 기존에는 created_at(매칭 시각) 기준으로 셌는데, 합성은 매칭보다 며칠 뒤에
-- 일어날 수 있다(상한에 걸린 후보는 다음 날로 넘어가므로 정상 동작). 그래서
-- "오늘 매칭된 주제" 2건을 채운 뒤에도 과거에 매칭된 주제를 계속 합성해서
-- 상한이 사실상 무력화됐다(2026-10-04 실측: 상한 2건인데 10/1에 7건 노출).
ALTER TABLE topic_pairs ADD COLUMN synthesized_at TIMESTAMPTZ;

-- 기존 행은 합성 시각을 알 수 없으므로 매칭 시각으로 채운다(대부분 같은 날 합성됨).
UPDATE topic_pairs SET synthesized_at = created_at WHERE title IS NOT NULL;
