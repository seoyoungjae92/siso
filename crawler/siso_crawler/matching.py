from .embedding import EmbeddingProvider
from .matching_repository import MatchingRepository

# 2026-07-22 실측(jhgan/ko-sbert-multitask, seed 12건 cross-side 36쌍 전수
# 비교): 실제 같은 주제(전기요금/주4일제/의료공백/플랫폼 규제) 4쌍은 유사도
# 0.5158~0.6119에 몰려있고, 무관한 주제 쌍은 최고 0.4490까지만 올라감 —
# 그 사이인 0.5로 시작. 표본이 작으니 초기 추정치이며, 데이터가 늘면 13절
# CMS 임계값 조정 기능으로 재튜닝할 것.
MATCH_SIMILARITY_THRESHOLD = 0.5

# 매칭용 임계값과 우연히 같은 값(0.5)으로 시작하지만, 관리자가 CMS에서
# 매칭 민감도만 따로 튜닝해도 정리(prune) 로직이 영향받지 않도록 개념을
# 분리한 별도 상수. 실제 운영값은 crawl_settings 테이블에서 읽어온다.
PRUNE_SIMILARITY_THRESHOLD = 0.5

# 코호트(같은 진영 내 "같은 이야기" 후보) 결속 임계값 — 매칭/정리
# 임계값과도 별도 개념(관리자가 코호트 결속력만 따로 튜닝해도 매칭/정리
# 로직이 영향받지 않도록)이라 같은 이유로 분리. 실제 운영값은
# crawl_settings 테이블에서 읽어온다.
COHORT_SIMILARITY_THRESHOLD = 0.5

# 코호트 하나에 몇 건까지 붙일지 상한. 원래는 무료 LLM 모델 컨텍스트/응답
# 길이 문제로 5였으나, 유료 고정 모델 전환 + 입장 요약 분량 축소(120~180자,
# 2026-09) 이후로는 그 제약이 사라짐 — synthesis_min_posts_per_side를 6
# 초과로 올리려면 이 상한도 같이 올려야 한다는 걸 알아채기 어려워서
# 사용자가 직접 발견한 문제(2026-09-15, 7로 올렸는데도 주제가 전부 정확히
# 6/6이던 것으로 드러남). 주제 생산량을 더 줄이기 위해 8로 상향.
MAX_COHORT_SIZE = 8

# 실제 운영값은 crawl_settings.synthesis_min_posts_per_side에서 읽어온다.
SYNTHESIS_MIN_POSTS_PER_SIDE_DEFAULT = 1

# 새 코호트가 최근 이 시간 이내 만들어진 활성 주제와 같은 이야기로 보이면
# (코호트 결속 임계값과 같은 기준 재사용) 새 topic_pair를 또 만들지 않고
# 그 기존 주제로 조용히 흡수시킨다 — 같은 이슈가 하루에도 여러 개의 별도
# 주제로 쪼개지던 중복 문제 완화(2026-09 사용자 발견). 너무 길면 며칠 전
# "끝난" 이슈에 오늘 글이 억지로 흡수될 수 있어 하루 정도로 제한.
DEDUP_LOOKBACK_HOURS = 24


def embed_pending_posts(
    repo: MatchingRepository, embedder: EmbeddingProvider, limit: int = 50
) -> int:
    pending = repo.find_posts_missing_embedding(limit)
    for post_id, title, summary in pending:
        embedding = embedder.embed(f"{title} {summary}")
        repo.update_embedding(post_id, embedding)
    return len(pending)


def match_pending_posts(
    repo: MatchingRepository,
    threshold: float = MATCH_SIMILARITY_THRESHOLD,
    cohort_threshold: float = COHORT_SIMILARITY_THRESHOLD,
    min_posts_per_side: int = SYNTHESIS_MIN_POSTS_PER_SIDE_DEFAULT,
) -> int:
    matched = 0
    # 이번 사이클에서 이미 어떤 코호트에 편입된 post_id — find_unmatched_posts가
    # 루프 시작 시점의 스냅샷이라, 코호트로 소비된 글이 뒤에서 다시 시드로
    # 뽑히거나 다른 코호트에 중복 편입되는 걸 막는다.
    consumed: set[int] = set()

    for post_id in repo.find_unmatched_posts("left"):
        if post_id in consumed:
            continue

        result = repo.find_best_cross_side_match(post_id)
        if result is None:
            continue
        right_id, similarity = result
        if similarity < threshold or right_id in consumed:
            continue

        left_cohort = [
            pid
            for pid, _ in repo.find_similar_same_side_posts(post_id, cohort_threshold, MAX_COHORT_SIZE)
            if pid not in consumed
        ]
        left_ids = [post_id] + left_cohort
        if len(left_ids) < min_posts_per_side:
            continue  # 우측 코호트 조회 전에 조기 종료 — 매 사이클 재시도되므로 비용 절감

        right_cohort = [
            pid
            for pid, _ in repo.find_similar_same_side_posts(right_id, cohort_threshold, MAX_COHORT_SIZE)
            if pid not in consumed
        ]
        right_ids = [right_id] + right_cohort
        if len(right_ids) < min_posts_per_side:
            continue

        existing = repo.find_recent_similar_pair(post_id, "left", cohort_threshold, DEDUP_LOOKBACK_HOURS)
        if existing is not None:
            existing_pair_id, _ = existing
            repo.attach_to_existing_pair(existing_pair_id, left_ids, right_ids)
        else:
            repo.create_pair(left_ids, right_ids, similarity)
            matched += 1
        consumed.update(left_ids)
        consumed.update(right_ids)

    return matched


def prune_stale_candidates(
    repo: MatchingRepository,
    grace_period_hours: int,
    min_cluster_size: int,
    limit: int,
    match_similarity_threshold: float = MATCH_SIMILARITY_THRESHOLD,
    prune_threshold: float = PRUNE_SIMILARITY_THRESHOLD,
) -> int:
    deleted = 0
    for post_id in repo.find_prunable_posts(grace_period_hours, match_similarity_threshold, limit):
        cluster_size = repo.count_similar_posts(post_id, prune_threshold) + 1  # 자기 자신 포함
        if cluster_size < min_cluster_size and repo.delete_post(post_id):
            deleted += 1

    return deleted


def delete_stale_posts(repo: MatchingRepository, retention_days: int, limit: int) -> int:
    """prune_stale_candidates와 별개의 단순 보관 기간 정책 — 매칭
    가능성(벡터 유사도)과 무관하게, display_window_days가 지나 어차피
    피드·플레이그라운드 어디에도 안 보이는 글을 그냥 지운다(2026-09,
    DB 용량 점검 중 사용자 요청으로 추가 — 매칭 안 된 글이 쌓여
    posts.embedding 인덱스가 DB 용량 대부분을 차지하고 있었음)."""
    deleted = 0
    for post_id in repo.find_stale_post_ids(retention_days, limit):
        if repo.delete_post(post_id):
            deleted += 1

    return deleted
