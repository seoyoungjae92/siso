# M1-1 진행 상황

브랜치: `feature/m1-1` (커밋 4개, 태스크 단위)

## 완료

1. **모노레포 스캐폴딩** — `/backend`(Spring Boot 4.1.0, Java 21, Gradle),
   `/frontend`(Next.js 16, TypeScript, Tailwind, App Router),
   `/crawler`(Python 3.12.11 venv), `/infra`(docker-compose, backend Dockerfile)
2. **DB 마이그레이션** — `backend/.../db/migration/V1__init.sql`, 12절 스키마 그대로.
   로컬 docker compose postgres(pgvector/pg16)에 적용 확인 완료
3. **RSS 크롤러 파이프라인** — `crawler/siso_crawler/` (parser/fetch/summarize/dedupe/
   repository/pipeline). fixture RSS로 pytest 9개 통과 (실네트워크 호출 없음)
4. **피드 조회 API** — `GET /api/posts?side=left|right&page&size`.
   서비스 레이어 유닛 테스트(Mockito) + 로컬 DB로 수동 curl 검증 + OpenAPI 스펙
   생성 확인(`backend/openapi.json`은 검증 시점 스냅샷)

## 사람이 할 일

- **실 배포 계정 생성**: Supabase(DB), Railway/Fly.io(백엔드), Vercel(프론트),
  Upstash/Railway Redis — 전부 미가입 상태. `.env.example`만 있고 실제 `.env`는 없음
- **GitHub Actions CI**: 10절/M1 로드맵엔 있지만 이번 작업 범위(17.0절 4개 항목)엔
  없어서 손대지 않음. 다음 세션에서 진행 필요
- **push / PR**: 로컬 커밋만 되어 있음. 원격 push 및 PR 생성은 사용자 확인 후 진행
  (작업 규칙 준수: 되돌리기 번거로운 외부 가시적 행동이라 임의로 하지 않음)
- **RSS 소스 미정**: 실제 크롤링 대상 커뮤니티 RSS URL 확정 필요 (현재는 fixture만 사용)
- **로컬 인프라 정리**: 검증에 사용한 `infra` docker compose(postgres+redis)가 로컬에
  계속 떠 있음. 필요 없으면 `cd infra && docker compose down` (볼륨까지 지우려면 `-v` 추가)

## 다음 (M1 잔여 범위, 이번 작업 아님)

CI 파이프라인, 소스 4~6개 확장, 키워드 매칭, 피드 PC UI 등은 로드맵 M1/M2 항목으로 별도 세션에서.
