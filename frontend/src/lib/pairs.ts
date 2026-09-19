import { BACKEND_API_URL } from "@/lib/posts";

// 토론 페이지 보강 필드(쟁점 배경 / 좌우 핵심 논거 / 생각해볼 질문).
// 보강 없는 주제는 background가 null이고 목록들은 빈 배열.
export type TopicEnrichment = {
  background: string | null;
  leftPoints: string[];
  rightPoints: string[];
  discussionQuestions: string[];
};

// 보강 없는 주제 페이지는 본문이 좌우 요약 몇 줄뿐이라 애드센스·검색엔진이
// "가치가 낮은 콘텐츠"로 보는 원인이 됨 — 이 판정으로 noindex/sitemap
// 제외를 결정한다.
export function isEnriched(pair: TopicEnrichment): boolean {
  return Boolean(pair.background);
}

export type TopicPair = TopicEnrichment & {
  id: number;
  title: string;
  leftStance: string;
  rightStance: string;
  createdAt: string;
  leftVotes: number;
  rightVotes: number;
  neutralVotes: number;
  voteCount: number;
  commentCount: number;
};

export function calculateVotePercentages(votes: {
  leftVotes: number;
  rightVotes: number;
  neutralVotes: number;
}): { leftPct: number; neutralPct: number; rightPct: number; total: number } {
  const total = votes.leftVotes + votes.rightVotes + votes.neutralVotes;
  if (total === 0) {
    return { leftPct: 0, neutralPct: 0, rightPct: 0, total: 0 };
  }
  const leftPct = Math.round((votes.leftVotes / total) * 100);
  const neutralPct = Math.round((votes.neutralVotes / total) * 100);
  const rightPct = Math.max(0, 100 - leftPct - neutralPct);
  return { leftPct, neutralPct, rightPct, total };
}

type PairsPage = {
  content: TopicPair[];
  last: boolean;
};

export type PairsResult = {
  pairs: TopicPair[];
  hasMore: boolean;
};

export async function fetchFeaturedPair(): Promise<TopicPair | null> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs/featured`, {
      // fetchPairs와 동일하게 개인화 데이터 없음, 동일 주기로 재검증.
      next: { revalidate: 30 },
    });
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchPairs(page = 0): Promise<PairsResult> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/pairs?page=${page}&size=10`, {
      // 개인화 데이터(myStance 등) 없는 목록 조회라 캐시해도 안전함 —
      // 새 주제는 크롤러(외부 프로세스)가 만들어서 revalidatePath로
      // 즉시 무효화할 방법이 없으니, 짧은 시간 기반 재검증으로 신선도와
      // 뒤로가기 속도를 절충한다.
      next: { revalidate: 30 },
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
