export type Side = "left" | "right";

export type PostSummary = {
  id: number;
  title: string;
  summary: string;
  sourceName: string;
  originUrl: string;
  publishedAt: string | null;
  collectedAt: string;
};

type PostsPage = {
  content: PostSummary[];
  last: boolean;
};

export type PostsResult = {
  posts: PostSummary[];
  hasMore: boolean;
};

export const BACKEND_API_URL = process.env.BACKEND_API_URL ?? "http://localhost:8080";

export async function fetchPosts(side: Side, page = 0): Promise<PostsResult> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/posts?side=${side}&page=${page}&size=20`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return { posts: [], hasMore: false };
    }

    const data: PostsPage = await res.json();
    return { posts: data.content, hasMore: !data.last };
  } catch {
    return { posts: [], hasMore: false };
  }
}
