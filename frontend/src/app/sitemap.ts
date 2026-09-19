import type { MetadataRoute } from "next";

import { fetchPairs, isEnriched } from "@/lib/pairs";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

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
