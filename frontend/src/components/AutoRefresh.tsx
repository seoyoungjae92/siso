"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

const REFRESH_INTERVAL_MS = 60_000;

/**
 * 크롤러가 새 글/링을 채워도 페이지를 계속 켜두고 있으면 반영이 안 되던
 * 문제 — 주기적으로 조용히 router.refresh()해서 서버 컴포넌트를 다시
 * 렌더링한다. 스크롤 위치는 유지되고, 새로 내려온 데이터는 FeedColumn/
 * Playground가 알아서 기존 목록 위에 병합한다.
 */
export function AutoRefresh() {
  const router = useRouter();

  useEffect(() => {
    const timer = setInterval(() => router.refresh(), REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [router]);

  return null;
}
