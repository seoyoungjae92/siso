"use client";

import { useEffect, useRef, useState, useTransition } from "react";

import { loadMorePosts } from "@/app/actions";
import { AdSlot } from "@/components/AdSlot";
import { PostCard } from "@/components/PostCard";
import type { PostSummary, Side } from "@/lib/posts";

const COLUMN: Record<Side, { bg: string; heading: string; title: string }> = {
  left: { bg: "bg-blue-tint", heading: "text-left-blue", title: "좌 성향 커뮤니티" },
  right: { bg: "bg-red-tint", heading: "text-right-red", title: "우 성향 커뮤니티" },
};

const AD_EVERY = 5;

export function FeedColumn({
  side,
  posts: initialPosts,
  hasMore: initialHasMore,
}: {
  side: Side;
  posts: PostSummary[];
  hasMore: boolean;
}) {
  const { bg, heading, title } = COLUMN[side];
  const [posts, setPosts] = useState(initialPosts);
  const [hasMore, setHasMore] = useState(initialHasMore);
  const [newIds, setNewIds] = useState<Set<number>>(new Set());
  const [isPending, startTransition] = useTransition();
  const sentinelRef = useRef<HTMLDivElement>(null);
  const postsRef = useRef(posts);
  // 페이지 커서를 ref로 관리 — IntersectionObserver 콜백이 리렌더 전에
  // 연달아 여러 번 발화해도(React state는 비동기 반영) 같은 페이지를
  // 중복 요청하지 않도록 동기적으로 증가시킴
  const cursor = useRef({ nextPage: 1, fetching: false });

  useEffect(() => {
    postsRef.current = posts;
  }, [posts]);

  // 주기적 자동 새로고침(AutoRefresh)으로 서버에서 새 initialPosts가
  // 내려오면, 이미 로딩된 목록(무한스크롤로 쌓인 것 포함) 맨 위에 진짜
  // 새 글만 얹는다 — 통째로 교체하면 무한스크롤로 불러온 게 날아감.
  useEffect(() => {
    const existingIds = new Set(postsRef.current.map((p) => p.id));
    const fresh = initialPosts.filter((p) => !existingIds.has(p.id));
    if (fresh.length === 0) return;

    setPosts((prev) => [...fresh, ...prev]);
    setNewIds(new Set(fresh.map((p) => p.id)));
    const timer = setTimeout(() => setNewIds(new Set()), 500);
    return () => clearTimeout(timer);
  }, [initialPosts]);

  useEffect(() => {
    if (!hasMore) return;
    const el = sentinelRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries[0].isIntersecting || cursor.current.fetching) return;
        cursor.current.fetching = true;
        const nextPage = cursor.current.nextPage;
        cursor.current.nextPage += 1;
        startTransition(async () => {
          try {
            const next = await loadMorePosts(side, nextPage);
            setPosts((prev) => [...prev, ...next.posts]);
            setHasMore(next.hasMore);
          } finally {
            cursor.current.fetching = false;
          }
        });
      },
      { rootMargin: "400px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasMore, side]);

  return (
    // min-h-full: 모바일 탭에서 이 section의 부모(overflow-y-auto 스크롤
    // 컨테이너)는 실측 뷰포트 높이를 갖는데, 게시글이 적으면 section이
    // 내용만큼만 높이를 차지해 그 아래로 틴트 배경 없이 흰 여백이 보임 —
    // 최소 부모 높이만큼은 채우도록 보정(데스크톱 grid에서는 이미 grid
    // stretch로 채워지고 있어 영향 없음).
    <section className={`min-h-full px-[18px] py-5 ${bg}`}>
      <div className="mb-3.5 flex items-baseline gap-2">
        <h2 className={`text-[15px] font-extrabold tracking-tight ${heading}`}>{title}</h2>
        <span className="text-xs text-[#8A877E]">실시간 수집</span>
      </div>

      {posts.length === 0 && (
        <p className="text-sm text-[#6B6960]">아직 수집된 글이 없습니다.</p>
      )}

      {posts.map((post, index) => (
        <div key={post.id} className={newIds.has(post.id) ? "animate-new-item" : ""}>
          <PostCard post={post} side={side} />
          {(index + 1) % AD_EVERY === 0 && <AdSlot position={`feed-${side}`} />}
        </div>
      ))}

      {hasMore && (
        <div ref={sentinelRef} className="flex h-6 items-center justify-center">
          {isPending && (
            <span className="text-xs text-[#8A877E]" role="status">
              불러오는 중...
            </span>
          )}
        </div>
      )}
    </section>
  );
}
