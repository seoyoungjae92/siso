import * as Sentry from "@sentry/nextjs";

// DSN이 없으면(계정 미생성 시점) Sentry SDK가 스스로 비활성화된다(공식 동작) —
// 다른 선택적 연동과 동일하게 키 없이도 조용히 꺼져 있다.
Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  tracesSampleRate: 0.1,
});
