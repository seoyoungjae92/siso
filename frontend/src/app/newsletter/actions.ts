"use server";

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

export async function postSubscribeNewsletter(email: string) {
  if (!email.trim()) {
    return { ok: false, error: "이메일을 입력해주세요." };
  }

  const res = await fetch(`${BACKEND_API_URL}/api/newsletter/subscribe`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email.trim() }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "구독 신청에 실패했습니다.") };
  }

  return { ok: true };
}

export async function postConfirmNewsletter(token: string) {
  const res = await fetch(`${BACKEND_API_URL}/api/newsletter/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "구독 확인에 실패했습니다.") };
  }

  return { ok: true };
}

export async function postUnsubscribeNewsletter(token: string) {
  const res = await fetch(`${BACKEND_API_URL}/api/newsletter/unsubscribe`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (!res.ok) {
    return { ok: false, error: await extractErrorMessage(res, "수신거부 처리에 실패했습니다.") };
  }

  return { ok: true };
}
