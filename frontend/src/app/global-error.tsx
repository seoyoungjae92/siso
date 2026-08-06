"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

// 루트 레이아웃 자체가 깨졌을 때 뜨는 최후 방어선이라, 레이아웃이 로드하는
// 폰트/CSS 파이프라인에 의존하지 않도록 인라인 스타일만 사용한다.
export default function GlobalError({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html lang="ko">
      <body style={{ fontFamily: "sans-serif", textAlign: "center", padding: "80px 20px" }}>
        <h1 style={{ fontSize: "22px", fontWeight: 800, marginBottom: "8px" }}>
          일시적인 오류가 발생했습니다
        </h1>
        <p style={{ fontSize: "14px", color: "#6B6960", marginBottom: "24px" }}>
          잠시 후 다시 시도해주세요.
        </p>
        <button
          type="button"
          onClick={() => unstable_retry()}
          style={{
            borderRadius: "999px",
            background: "#6E3D74",
            color: "white",
            padding: "8px 20px",
            fontSize: "14px",
            fontWeight: 700,
            border: "none",
          }}
        >
          다시 시도
        </button>
      </body>
    </html>
  );
}
