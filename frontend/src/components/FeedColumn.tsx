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
  const [, startTransition] = useTransition();
  const sentinelRef = useRef<HTMLDivElement>(null);
  // 페이지 커서를 ref로 관리 — IntersectionObserver 콜백이 리렌더 전에
  // 연달아 여러 번 발화해도(React state는 비동기 반영) 같은 페이지를
  // 중복 요청하지 않도록 동기적으로 증가시킴
  const cursor = useRef({ nextPage: 1, fetching: false });

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
    <section className={`px-[18px] py-5 ${bg}`}>
      <div className="mb-3.5 flex items-baseline gap-2">
        <h2 className={`text-[15px] font-extrabold tracking-tight ${heading}`}>{title}</h2>
        <span className="text-xs text-[#8A877E]">실시간 수집</span>
      </div>

      {posts.map((post, index) => (
        <div key={post.id}>
          <PostCard post={post} side={side} />
          {(index + 1) % AD_EVERY === 0 && <AdSlot position={`feed-${side}`} />}
        </div>
      ))}

      {hasMore && <div ref={sentinelRef} aria-hidden className="h-1" />}
    </section>
  );
}
