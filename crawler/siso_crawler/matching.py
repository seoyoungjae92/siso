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


def embed_pending_posts(
    repo: MatchingRepository, embedder: EmbeddingProvider, limit: int = 50
) -> int:
    pending = repo.find_posts_missing_embedding(limit)
    for post_id, title, summary in pending:
        embedding = embedder.embed(f"{title} {summary}")
        repo.update_embedding(post_id, embedding)
    return len(pending)


def match_pending_posts(
    repo: MatchingRepository, threshold: float = MATCH_SIMILARITY_THRESHOLD
) -> int:
    matched = 0
    for post_id in repo.find_unmatched_posts("left"):
        result = repo.find_best_cross_side_match(post_id)
        if result is None:
            continue

        right_id, similarity = result
        if similarity >= threshold:
            repo.create_pair(post_id, right_id, similarity)
            matched += 1

    return matched


def prune_stale_candidates(
    repo: MatchingRepository,
    grace_period_hours: int,
    min_cluster_size: int,
    prune_threshold: float = PRUNE_SIMILARITY_THRESHOLD,
) -> int:
    deleted = 0
    for post_id in repo.find_prunable_posts(grace_period_hours):
        cluster_size = repo.count_similar_posts(post_id, prune_threshold) + 1  # 자기 자신 포함
        if cluster_size < min_cluster_size and repo.delete_post(post_id):
            deleted += 1

    return deleted
