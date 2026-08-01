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
  synthesisLimit: number;
  synthesisModel: string;
  deadLinkScanLimit: number;
  pruneScanLimit: number;
  sourceFailureThreshold: number;
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

export type ModerationSettingsInput = {
  autoBlindReportThreshold: number;
  classificationModel: string;
};

export async function postUpdateModerationSettings(input: ModerationSettingsInput) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/moderation-settings`, {
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

export type ElectionSettingsInput = {
  enabled: boolean;
  overrideAutoBlindThreshold: number;
};

export async function postUpdateElectionSettings(input: ElectionSettingsInput) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/election-settings`, {
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
  revalidatePath("/");
  return { ok: true };
}

export type AbuseSettingsInput = {
  multiAccountClusterSize: number;
  multiAccountTrustPenaltyMultiplier: number;
  trustMaturityHours: number;
  trustMinWeight: number;
  duplicateSimilarityThreshold: number;
  duplicateLookbackCount: number;
  duplicateLookbackMinutes: number;
  spikeWindowMinutes: number;
  spikeVoteThreshold: number;
  spikeReactionThreshold: number;
};

export async function postUpdateAbuseSettings(input: AbuseSettingsInput) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/abuse-settings`, {
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

export type PetitionSettingsInput = {
  eraco: string;
  topN: number;
  windowDays: number;
};

export async function postUpdatePetitionSettings(input: PetitionSettingsInput) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/petition-settings`, {
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
