"use server";

import { revalidatePath } from "next/cache";

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

export async function postComment(
  pairId: string,
  body: string,
  parentId?: number,
  stance?: string,
) {
  const anonId = await getAnonId();
  if (!anonId) {
    return { ok: false, error: "익명 ID가 없습니다. 새로고침 후 다시 시도해주세요." };
  }
  if (!body.trim()) {
    return { ok: false, error: "내용을 입력해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}/comments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Anon-Id": anonId,
    },
    body: JSON.stringify({ body, parentId, stance }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "댓글 작성에 실패했습니다.") };
  }

  revalidatePath(`/pairs/${pairId}`);
  return { ok: true };
}

export async function postReaction(pairId: string, commentId: number, type: "up" | "down") {
  const anonId = await getAnonId();
  if (!anonId) {
    return { ok: false, error: "익명 ID가 없습니다. 새로고침 후 다시 시도해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/comments/${commentId}/reactions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Anon-Id": anonId,
    },
    body: JSON.stringify({ type }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "추천 처리에 실패했습니다.") };
  }

  revalidatePath(`/pairs/${pairId}`);
  return { ok: true };
}

export async function postReport(
  pairId: string,
  commentId: number,
  reason: "abuse" | "hate" | "spam" | "etc",
  detail?: string,
) {
  const anonId = await getAnonId();
  if (!anonId) {
    return { ok: false, error: "익명 ID가 없습니다. 새로고침 후 다시 시도해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/comments/${commentId}/reports`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Anon-Id": anonId,
    },
    body: JSON.stringify({ reason, detail }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "신고 처리에 실패했습니다.") };
  }

  revalidatePath(`/pairs/${pairId}`);
  return { ok: true };
}

export async function postVote(pairId: string, stance: "left" | "right" | "neutral") {
  const anonId = await getAnonId();
  if (!anonId) {
    return { ok: false, error: "익명 ID가 없습니다. 새로고침 후 다시 시도해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}/votes`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Anon-Id": anonId,
    },
    body: JSON.stringify({ stance }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "투표 처리에 실패했습니다.") };
  }

  revalidatePath(`/pairs/${pairId}`);
  return { ok: true };
}
