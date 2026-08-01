FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
# GPU 없는 크론 잡이라 CUDA 포함 torch(수 GB)는 낭비 — CPU 전용 wheel을
# 먼저 깔아서 이후 sentence-transformers가 이걸 그대로 쓰게 함.
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch
RUN pip install --no-cache-dir -r requirements.txt

COPY siso_crawler ./siso_crawler

# 임베딩 모델을 빌드 타임에 캐시해둠 — 런타임에 HF_HUB_OFFLINE=1로 막아도
# 첫 실행이 실패하지 않게 하기 위함(런타임 네트워크 메타데이터 조회가
# 실패해서 크롤이 죽는 문제를 로컬에서 실제로 겪음).
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('jhgan/ko-sbert-multitask')"

ENV HF_HUB_OFFLINE=1

CMD ["python", "-m", "siso_crawler.run"]
