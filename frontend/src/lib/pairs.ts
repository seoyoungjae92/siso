import { BACKEND_API_URL, type PostSummary } from "@/lib/posts";

export type TopicPair = {
  id: number;
  similarity: number;
  createdAt: string;
  leftPost: PostSummary;
  rightPost: PostSummary;
};

type PairsPage = {
  content: TopicPair[];
  last: boolean;
};

export type PairsResult = {
  pairs: TopicPair[];
  hasMore: boolean;
};

export async function fetchPairs(page = 0): Promise<PairsResult> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs?page=${page}&size=10`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return { pairs: [], hasMore: false };
    }

    const data: PairsPage = await res.json();
    return { pairs: data.content, hasMore: !data.last };
  } catch {
    return { pairs: [], hasMore: false };
  }
}
