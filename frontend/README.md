# siso — frontend

Next.js 16 (App Router) 기반 프론트엔드입니다.
프로젝트 전체 소개·실행 방법·아키텍처는 저장소 루트의 [README.md](../README.md)를 보세요.

```bash
npm install
npm run dev     # http://localhost:3000
```

환경변수는 루트 `.env.example` 참고. `BACKEND_API_URL`이 백엔드(기본 `http://localhost:8080`)를
가리켜야 하고, `ANON_ID_SIGNING_SECRET`은 백엔드와 **같은 값**이어야 합니다(익명 ID 서명 검증).

- `src/app` — 라우트(서버 컴포넌트), 서버 액션, 관리자 화면
- `src/components` — 좌·우 대칭 컴포넌트(`side: 'left' | 'right'` prop으로 재사용)
- `src/lib` — 백엔드 호출, 익명 ID 서명, 포맷 유틸
- `src/proxy.ts` — 익명 ID 쿠키 발급, `/admin` Basic 인증
