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
};

export async function fetchPairs(): Promise<TopicPair[]> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs?size=10`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return [];
    }

    const page: PairsPage = await res.json();
    return page.content;
  } catch {
    return [];
  }
}
