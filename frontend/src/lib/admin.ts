import { headers } from "next/headers";

import { BACKEND_API_URL } from "@/lib/posts";

export function adminAuthHeader(): string {
  const user = process.env.ADMIN_USERNAME ?? "admin";
  const pass = process.env.ADMIN_PASSWORD ?? "local-dev-only-password";
  return "Basic " + Buffer.from(`${user}:${pass}`).toString("base64");
}

// proxy.ts가 /admin 페이지 진입은 막아주지만, Server Action은 미들웨어
// 매칭과 무관하게 직접 호출 가능한 별도 엔드포인트라 액션 안에서도
// 다시 검증한다.
export async function requireAdmin() {
  const requestHeaders = await headers();
  if (requestHeaders.get("authorization") !== adminAuthHeader()) {
    throw new Error("admin auth required");
  }
}

export type PendingReportGroup = {
  commentId: number;
  commentBody: string;
  nickname: string;
  pairId: number | null;
  reasonCounts: Record<string, number>;
  totalReports: number;
  oldestReportAt: string;
};

export async function fetchPendingReports(): Promise<PendingReportGroup[]> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/reports`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return [];
  return res.json();
}

export type BlindHistoryEntry = {
  alertId: number;
  type: "comment_auto_blinded" | "comment_manually_blinded";
  commentId: number | null;
  commentBody: string | null;
  nickname: string | null;
  pairId: number | null;
  reportCount: number | null;
  createdAt: string;
};

export async function fetchBlindHistory(): Promise<BlindHistoryEntry[]> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/reports/history`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return [];
  return res.json();
}

export type Source = {
  id: number;
  name: string;
  side: "left" | "right";
  baseUrl: string;
  feedUrl: string | null;
  crawlType: "rss" | "html";
  enabled: boolean;
  createdAt: string;
};

export async function fetchSources(): Promise<Source[]> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/sources`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return [];
  return res.json();
}

export type SourceStat = {
  sourceName: string;
  side: "left" | "right";
  postCount: number;
  lastCollectedAt: string | null;
};

export type DashboardData = {
  totalComments: number;
  totalVotes: number;
  pendingReports: number;
  totalReports: number;
  newAnonUsersLast24h: number;
  activeAnonUsersLast24h: number;
  sourceStats: SourceStat[];
};

export async function fetchDashboard(): Promise<DashboardData | null> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/dashboard`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return null;
  return res.json();
}

export type CrawlSettings = {
  matchSimilarityThreshold: number;
  pruneSimilarityThreshold: number;
  minClusterSize: number;
  gracePeriodHours: number;
  displayWindowDays: number;
  updatedAt: string;
};

export async function fetchCrawlSettings(): Promise<CrawlSettings | null> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/crawl-settings`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return null;
  return res.json();
}

export type ModerationSettings = {
  autoBlindReportThreshold: number;
  updatedAt: string;
};

export async function fetchModerationSettings(): Promise<ModerationSettings | null> {
  const res = await fetch(`${BACKEND_API_URL}/api/admin/moderation-settings`, {
    cache: "no-store",
    headers: { Authorization: adminAuthHeader() },
  });
  if (!res.ok) return null;
  return res.json();
}
