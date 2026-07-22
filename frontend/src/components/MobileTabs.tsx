"use client";

import { useRef, useState } from "react";

import { FeedColumn } from "@/components/FeedColumn";
import { Playground } from "@/components/Playground";
import type { TopicPair } from "@/lib/pairs";
import type { PostSummary } from "@/lib/posts";

type Tab = "left" | "playground" | "right";

const TAB_CONFIG: Record<Tab, { label: string; text: string; bar: string }> = {
  left: { label: "좌", text: "text-left-blue", bar: "bg-left-blue" },
  playground: { label: "플레이그라운드", text: "text-playground", bar: "bg-playground" },
  right: { label: "우", text: "text-right-red", bar: "bg-right-red" },
};

const TABS: Tab[] = ["left", "playground", "right"];

export function MobileTabs({
  leftPosts,
  rightPosts,
  pairs,
}: {
  leftPosts: PostSummary[];
  rightPosts: PostSummary[];
  pairs: TopicPair[];
}) {
  const [tab, setTab] = useState<Tab>("playground");
  const touchStart = useRef<{ x: number; y: number } | null>(null);

  function handleTouchStart(e: React.TouchEvent) {
    const t = e.touches[0];
    touchStart.current = { x: t.clientX, y: t.clientY };
  }

  function handleTouchEnd(e: React.TouchEvent) {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start) return;

    const t = e.changedTouches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;

    const SWIPE_THRESHOLD = 60;
    if (Math.abs(dx) < SWIPE_THRESHOLD || Math.abs(dx) < Math.abs(dy)) return;

    const currentIndex = TABS.indexOf(tab);
    const nextIndex = currentIndex + (dx < 0 ? 1 : -1);
    const clamped = Math.max(0, Math.min(TABS.length - 1, nextIndex));
    if (clamped !== currentIndex) setTab(TABS[clamped]);
  }

  return (
    <div className="flex flex-1 flex-col">
      <div
        className="flex-1 overflow-y-auto"
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        {/* 스와이프: 좌 ↔ 플레이그라운드 ↔ 우 순서로 좌우로 밀어 이동.
            세로 스크롤과 헷갈리지 않게 가로 이동량이 세로보다 클 때만 반응 */}
        {tab === "left" && <FeedColumn side="left" posts={leftPosts} />}
        {tab === "playground" && (
          <div className="bg-pg-tint px-[14px] py-4">
            <Playground pairs={pairs} />
          </div>
        )}
        {tab === "right" && <FeedColumn side="right" posts={rightPosts} />}
      </div>

      <div className="sticky bottom-0 grid grid-cols-3 border-t border-line bg-white">
        {TABS.map((t) => {
          const config = TAB_CONFIG[t];
          const active = t === tab;
          return (
            <button
              key={t}
              type="button"
              onClick={() => setTab(t)}
              className={`relative py-2.5 pb-3.5 text-[11px] font-extrabold ${active ? config.text : "text-[#A09D94]"}`}
            >
              {active && (
                <span className={`absolute inset-x-[20%] top-0 h-[3px] rounded-b ${config.bar}`} />
              )}
              {config.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
