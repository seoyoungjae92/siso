-- 수신거부 시 레코드를 삭제하는 방식으로 바뀌어(status='unsubscribed'로
-- 남기지 않음) 이 컬럼을 더 이상 쓰지 않는다.
ALTER TABLE newsletter_subscribers DROP COLUMN unsubscribed_at;
