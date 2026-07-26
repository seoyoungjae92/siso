"use client";

import { useEffect, useRef, useState, useTransition } from "react";

import { loadMorePairs } from "@/app/actions";
import { AdSlot } from "@/components/AdSlot";
import { PairCard } from "@/components/PairCard";
import { PetitionWidget } from "@/components/PetitionWidget";
import type { TopicPair } from "@/lib/pairs";
import type { Petition } from "@/lib/petitions";

const AD_EVERY = 3;

export function Playground({
  pairs: initialPairs,
  hasMore: initialHasMore,
  petitions,
  hideVotes = false,
}: {
  pairs: TopicPair[];
  hasMore: boolean;
  petitions: Petition[];
  hideVotes?: boolean;
}) {
  const [pairs, setPairs] = useState(initialPairs);
  const [hasMore, setHasMore] = useState(initialHasMore);
  const [newIds, setNewIds] = useState<Set<number>>(new Set());
  const [, startTransition] = useTransition();
  const sentinelRef = useRef<HTMLDivElement>(null);
  const pairsRef = useRef(pairs);
  const cursor = useRef({ nextPage: 1, fetching: false });

  useEffect(() => {
    pairsRef.current = pairs;
  }, [pairs]);

  // 주기적 자동 새로고침(AutoRefresh)으로 서버에서 새 initialPairs가
  // 내려오면, 이미 로딩된 목록 맨 위에 진짜 새 주제만 얹는다 — 통째로
  // 교체하면 무한스크롤로 불러온 게 날아감.
  useEffect(() => {
    const existingIds = new Set(pairsRef.current.map((p) => p.id));
    const fresh = initialPairs.filter((p) => !existingIds.has(p.id));
    if (fresh.length === 0) return;

    setPairs((prev) => [...fresh, ...prev]);
    setNewIds(new Set(fresh.map((p) => p.id)));
    const timer = setTimeout(() => setNewIds(new Set()), 500);
    return () => clearTimeout(timer);
  }, [initialPairs]);

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
    return (
      <>
        <PetitionWidget petitions={petitions} />
        <p className="text-sm text-[#6B6960]">아직 매칭된 주제가 없습니다.</p>
      </>
    );
  }

  const [today, ...rest] = pairs;

  return (
    <>
      <PetitionWidget petitions={petitions} />
      {hideVotes && (
        <p className="mb-2.5 rounded-lg border border-line bg-[#F5F4F0] px-3 py-2 text-[11px] text-[#6B6960]">
          선거 기간 중에는 투표 기능이 일시 중단됩니다.
        </p>
      )}
      <div className="mb-2.5 flex items-center justify-end gap-3 text-[11px] text-[#8A877E]">
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-left-blue" />좌
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-playground" />중립
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-right-red" />우
        </span>
      </div>
      <div className={newIds.has(today.id) ? "animate-new-item" : ""}>
        <PairCard pair={today} large hideVotes={hideVotes} />
      </div>
      {rest.map((pair, index) => (
        <div key={pair.id} className={newIds.has(pair.id) ? "animate-new-item" : ""}>
          <PairCard pair={pair} hideVotes={hideVotes} />
          {(index + 1) % AD_EVERY === 0 && <AdSlot position="playground" />}
        </div>
      ))}

      {hasMore && <div ref={sentinelRef} aria-hidden className="h-1" />}
    </>
  );
}
