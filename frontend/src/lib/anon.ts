import { createHmac } from "crypto";
import { cookies, headers } from "next/headers";

const ANON_ID_COOKIE = "anon_id";

export async function getAnonId(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(ANON_ID_COOKIE)?.value;
}

// 백엔드가 X-Anon-Id를 그냥 신뢰하면 아무 UUID나 위조해 레이트리밋/중복필터를
// 우회할 수 있어서, 이 쿠키를 실제로 가진 요청인지 서명으로 증명한다. 여기 더해
// Vercel 엣지가 아는 실제 방문자 IP(x-forwarded-for)도 같이 넘겨준다 — 백엔드가
// Server Action 호출을 받을 땐 항상 Vercel 서버 IP만 보여서 IP 기반 어뷰징
// 탐지가 원래 무력화돼 있었음.
export async function getSignedAnonHeaders(): Promise<Record<string, string> | null> {
  const anonId = await getAnonId();
  if (!anonId) return null;

  const secret = process.env.ANON_ID_SIGNING_SECRET;
  if (!secret) {
    throw new Error("ANON_ID_SIGNING_SECRET 미설정");
  }
  const signature = createHmac("sha256", secret).update(anonId).digest("hex");

  const requestHeaders = await headers();
  const clientIp = requestHeaders.get("x-forwarded-for")?.split(",")[0]?.trim();

  return {
    "X-Anon-Id": anonId,
    "X-Anon-Sig": signature,
    ...(clientIp ? { "X-Client-Ip": clientIp } : {}),
  };
}
