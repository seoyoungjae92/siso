from .embedding import EmbeddingProvider
from .matching_repository import MatchingRepository

# 2026-07-22 실측(jhgan/ko-sbert-multitask, seed 12건 cross-side 36쌍 전수
# 비교): 실제 같은 주제(전기요금/주4일제/의료공백/플랫폼 규제) 4쌍은 유사도
# 0.5158~0.6119에 몰려있고, 무관한 주제 쌍은 최고 0.4490까지만 올라감 —
# 그 사이인 0.5로 시작. 표본이 작으니 초기 추정치이며, 데이터가 늘면 13절
# CMS 임계값 조정 기능으로 재튜닝할 것.
MATCH_SIMILARITY_THRESHOLD = 0.5


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
