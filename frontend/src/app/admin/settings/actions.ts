"use server";

import { revalidatePath } from "next/cache";

import { adminAuthHeader, requireAdmin } from "@/lib/admin";
import { BACKEND_API_URL } from "@/lib/posts";

async function extractErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json();
    if (typeof body?.message === "string" && body.message.length > 0) {
      return body.message;
    }
  } catch {
    // 응답 본문이 JSON이 아니면 fallback 사용
  }
  return fallback;
}

export type CrawlSettingsInput = {
  matchSimilarityThreshold: number;
  pruneSimilarityThreshold: number;
  minClusterSize: number;
  gracePeriodHours: number;
  displayWindowDays: number;
};

export async function postUpdateCrawlSettings(input: CrawlSettingsInput) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/crawl-settings`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: adminAuthHeader(),
    },
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "처리에 실패했습니다.") };
  }

  revalidatePath("/admin/settings");
  return { ok: true };
}
