"use server";

import { getAnonId } from "@/lib/anon";
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

export async function postFeedback(category: string, body: string, contact: string) {
  const anonId = await getAnonId();
  if (!anonId) {
    return { ok: false, error: "익명 ID가 없습니다. 새로고침 후 다시 시도해주세요." };
  }
  if (!body.trim()) {
    return { ok: false, error: "내용을 입력해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/feedback`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Anon-Id": anonId,
    },
    body: JSON.stringify({ category, body, contact: contact.trim() || null }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "제출에 실패했습니다.") };
  }

  return { ok: true };
}
