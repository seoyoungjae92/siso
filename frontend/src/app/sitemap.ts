import type { MetadataRoute } from "next";

import { fetchPairs, isEnriched } from "@/lib/pairs";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

// 빌드 시점에 한 번 prerender된 사이트맵이 fetch의 revalidate(30초)에도
// 불구하고 Vercel에서 갱신되지 않았음(2026-09-19, 보강 주제 백필 후에도
// 배포 시점의 0건짜리 사이트맵이 계속 HIT) — 요청 시점에 생성하도록 강제.
// 백엔드 호출은 fetchPairs의 데이터 캐시(30초)를 그대로 타므로 부담 없음.
export const dynamic = "force-dynamic";

// 사이클마다 계속 늘어나는 토론 주제를 무한정 다 담지 않도록 상한을 둠 —
// 최근 것부터(fetchPairs가 createdAt DESC로 정렬) 담기면 충분함.
const MAX_PAIR_PAGES = 10;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: siteUrl, changeFrequency: "hourly", priority: 1 },
    { url: `${siteUrl}/about`, changeFrequency: "monthly", priority: 0.5 },
    { url: `${siteUrl}/privacy`, changeFrequency: "yearly", priority: 0.1 },
    { url: `${siteUrl}/terms`, changeFrequency: "yearly", priority: 0.1 },
  ];

  const pairRoutes: MetadataRoute.Sitemap = [];
  for (let page = 0; page < MAX_PAIR_PAGES; page++) {
    const { pairs, hasMore } = await fetchPairs(page);
    for (const pair of pairs) {
      // 보강 없는 주제 페이지는 noindex라(pairs/[id]/page.tsx) sitemap에도 넣지 않는다.
      if (!isEnriched(pair)) continue;
      pairRoutes.push({
        url: `${siteUrl}/pairs/${pair.id}`,
        lastModified: pair.createdAt,
        changeFrequency: "daily",
        priority: 0.7,
      });
    }
    if (!hasMore) break;
  }

  return [...staticRoutes, ...pairRoutes];
}
