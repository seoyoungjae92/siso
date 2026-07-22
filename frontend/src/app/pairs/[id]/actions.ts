"use server";

import { revalidatePath } from "next/cache";

import { getAnonId } from "@/lib/anon";
import { BACKEND_API_URL } from "@/lib/posts";

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
    return { ok: false, error: "댓글 작성에 실패했습니다." };
  }

  revalidatePath(`/pairs/${pairId}`);
  return { ok: true };
}
