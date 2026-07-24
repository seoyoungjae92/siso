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

export async function fetchPairDetail(pairId: string): Promise<TopicPairDetail | null> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}`, {
      cache: "no-store",
      headers: await anonHeaders(),
    });
    if (!res.ok) {
      return null;
    }
    return await res.json();
  } catch {
    return null;
  }
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
