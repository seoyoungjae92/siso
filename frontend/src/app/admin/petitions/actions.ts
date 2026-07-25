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

export async function postSyncPetitions() {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/petitions/sync`, {
    method: "POST",
    headers: { Authorization: adminAuthHeader() },
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "동기화에 실패했습니다.") };
  }

  const result: { upserted: number; closed: number } = await res.json();
  revalidatePath("/admin/petitions");
  return { ok: true, upserted: result.upserted, closed: result.closed };
}
