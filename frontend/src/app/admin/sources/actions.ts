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

export type SourceInput = {
  name: string;
  side: string;
  baseUrl: string;
  feedUrl: string;
  crawlType: string;
};

async function postSourceRequest(url: string, method: "POST" | "PUT", input: SourceInput) {
  await requireAdmin();

  const res = await fetch(url, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: adminAuthHeader(),
    },
    body: JSON.stringify({
      name: input.name,
      side: input.side,
      baseUrl: input.baseUrl,
      feedUrl: input.feedUrl || null,
      crawlType: input.crawlType,
    }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "처리에 실패했습니다.") };
  }

  revalidatePath("/admin/sources");
  return { ok: true };
}

export async function postCreateSource(input: SourceInput) {
  return postSourceRequest(`${BACKEND_API_URL}/api/admin/sources`, "POST", input);
}

export async function postUpdateSource(id: number, input: SourceInput) {
  return postSourceRequest(`${BACKEND_API_URL}/api/admin/sources/${id}`, "PUT", input);
}

export async function postToggleSource(id: number) {
  await requireAdmin();

  const res = await fetch(`${BACKEND_API_URL}/api/admin/sources/${id}/toggle`, {
    method: "POST",
    headers: { Authorization: adminAuthHeader() },
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "처리에 실패했습니다.") };
  }

  revalidatePath("/admin/sources");
  return { ok: true };
}
