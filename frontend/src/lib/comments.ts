import { BACKEND_API_URL, type PostSummary } from "@/lib/posts";

export type TopicPairDetail = {
  id: number;
  similarity: number;
  createdAt: string;
  leftPost: PostSummary;
  rightPost: PostSummary;
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
  createdAt: string;
};

export async function fetchPairDetail(pairId: string): Promise<TopicPairDetail | null> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs/${pairId}`, { cache: "no-store" });
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
    });
    if (!res.ok) {
      return [];
    }
    return await res.json();
  } catch {
    return [];
  }
}
