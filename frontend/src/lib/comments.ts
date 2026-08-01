import { getAnonId } from "@/lib/anon";
import { BACKEND_API_URL } from "@/lib/posts";

export type TopicPairDetail = {
  id: number;
  title: string;
  leftStance: string;
  rightStance: string;
  createdAt: string;
  leftVotes: number;
  rightVotes: number;
  neutralVotes: number;
  commentCount: number;
  myStance: "left" | "right" | "neutral" | null;
};

export type Comment = {
  id: number;
  parentId: number | null;
  nickname: string;
  body: string;
  stance: "left" | "right" | "neutral" | null;
  upCount: number;
  downCount: number;
  selfReply: boolean;
  myReaction: "up" | "down" | null;
  blinded: boolean;
  createdAt: string;
};

async function anonHeaders(): Promise<HeadersInit> {
  const anonId = await getAnonId();
  return anonId ? { "X-Anon-Id": anonId } : {};
}

// null은 "이 토론은 실제로 존재하지 않음"(백엔드 404)만 의미한다. 그 외
// 실패(백엔드 장애, 네트워크 오류)는 그대로 던져서 error.tsx가 처리하게
// 한다 — 예전엔 전부 null로 뭉뚱그려서 notFound()가 호출되는 바람에,
// 백엔드가 잠깐 장애나도 검색엔진이 정상 페이지를 영구 삭제로 오인할
// 위험이 있었다(SEO 손상).
export async function fetchPairDetail(pairId: string): Promise<TopicPairDetail | null> {
  const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}`, {
    cache: "no-store",
    headers: await anonHeaders(),
  });
  if (res.status === 404) {
    return null;
  }
  if (!res.ok) {
    throw new Error(`토론 정보를 불러오지 못했습니다 (${res.status})`);
  }
  return await res.json();
}

export async function fetchComments(pairId: string): Promise<Comment[]> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}/comments`, {
      cache: "no-store",
      headers: await anonHeaders(),
    });
    if (!res.ok) {
      return [];
    }
    return await res.json();
  } catch {
    return [];
  }
}
