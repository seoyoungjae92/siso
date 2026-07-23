"use client";

import { useEffect, useRef, useState, useTransition } from "react";

import { loadMorePairs } from "@/app/actions";
import { AdSlot } from "@/components/AdSlot";
import { PairCard } from "@/components/PairCard";
import type { TopicPair } from "@/lib/pairs";

const AD_EVERY = 5;

export function Playground({
  pairs: initialPairs,
  hasMore: initialHasMore,
}: {
  pairs: TopicPair[];
  hasMore: boolean;
}) {
  const [pairs, setPairs] = useState(initialPairs);
  const [hasMore, setHasMore] = useState(initialHasMore);
  const [, startTransition] = useTransition();
  const sentinelRef = useRef<HTMLDivElement>(null);
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
            const next = await loadMorePairs(nextPage);
            setPairs((prev) => [...prev, ...next.pairs]);
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
  }, [hasMore]);

  if (pairs.length === 0) {
    return <p className="text-sm text-[#6B6960]">아직 매칭된 주제가 없습니다.</p>;
  }

  const [today, ...rest] = pairs;

  return (
    <>
      <PairCard pair={today} large />
      {rest.map((pair, index) => (
        <div key={pair.id}>
          <PairCard pair={pair} />
          {(index + 1) % AD_EVERY === 0 && <AdSlot position="playground" />}
        </div>
      ))}

      {hasMore && <div ref={sentinelRef} aria-hidden className="h-1" />}
    </>
  );
}
