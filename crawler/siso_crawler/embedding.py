from typing import Protocol

EMBEDDING_MODEL_NAME = "jhgan/ko-sbert-multitask"
EMBEDDING_DIM = 768


class EmbeddingProvider(Protocol):
    def embed(self, text: str) -> list[float]: ...


class SentenceTransformerEmbeddingProvider:
    def __init__(self, model_name: str = EMBEDDING_MODEL_NAME):
        from sentence_transformers import SentenceTransformer

        self._model = SentenceTransformer(model_name)

    def embed(self, text: str) -> list[float]:
        return self._model.encode(text, normalize_embeddings=True).tolist()
