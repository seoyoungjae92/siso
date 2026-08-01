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
      // 개인화 데이터 없는 목록 조회라 캐시해도 안전함 — 새 글은
      // 크롤러(외부 프로세스)가 만들어서 revalidatePath로 즉시 무효화할
      // 방법이 없으니, 짧은 시간 기반 재검증으로 신선도와 뒤로가기
      // 속도를 절충한다.
      next: { revalidate: 30 },
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
