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
};

const BACKEND_API_URL = process.env.BACKEND_API_URL ?? "http://localhost:8080";

export async function fetchPosts(side: Side): Promise<PostSummary[]> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/posts?side=${side}&size=20`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return [];
    }

    const page: PostsPage = await res.json();
    return page.content;
  } catch {
    return [];
  }
}
