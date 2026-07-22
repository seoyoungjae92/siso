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
  const [index, setIndex] = useState(TABS.indexOf("playground"));
  const [dragPx, setDragPx] = useState(0);
  const [dragging, setDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const gesture = useRef<{ startX: number; startY: number; horizontal: boolean | null } | null>(
    null,
  );

  function handleTouchStart(e: React.TouchEvent) {
    const t = e.touches[0];
    gesture.current = { startX: t.clientX, startY: t.clientY, horizontal: null };
    setDragging(true);
  }

  function handleTouchMove(e: React.TouchEvent) {
    const g = gesture.current;
    if (!g) return;
    const t = e.touches[0];
    const dx = t.clientX - g.startX;
    const dy = t.clientY - g.startY;

    if (g.horizontal === null) {
      if (Math.abs(dx) < 8 && Math.abs(dy) < 8) return;
      g.horizontal = Math.abs(dx) > Math.abs(dy);
    }
    if (!g.horizontal) return;

    // 맨 끝 탭에서 더 당기면 저항감(러버밴드)을 주고 그 이상은 안 넘어가게
    let next = dx;
    if (index === 0 && next > 0) next *= 0.35;
    if (index === TABS.length - 1 && next < 0) next *= 0.35;
    setDragPx(next);
  }

  function handleTouchEnd() {
    const g = gesture.current;
    gesture.current = null;
    setDragging(false);

    if (g?.horizontal) {
      const width = containerRef.current?.getBoundingClientRect().width ?? 1;
      const threshold = width * 0.18;
      if (dragPx <= -threshold && index < TABS.length - 1) {
        setIndex(index + 1);
      } else if (dragPx >= threshold && index > 0) {
        setIndex(index - 1);
      }
    }
    setDragPx(0);
  }

  return (
    <div className="flex flex-1 flex-col">
      <div
        ref={containerRef}
        className="relative flex-1 overflow-hidden"
        style={{ touchAction: "pan-y" }}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <div
          className="flex h-full"
          style={{
            width: "300%",
            transform: `translateX(calc(${(-index * 100) / 3}% + ${dragPx}px))`,
            transition: dragging ? "none" : "transform 280ms cubic-bezier(0.22, 1, 0.36, 1)",
          }}
        >
          <div className="h-full w-1/3 shrink-0 overflow-y-auto">
            <FeedColumn side="left" posts={leftPosts} />
          </div>
          <div className="h-full w-1/3 shrink-0 overflow-y-auto bg-pg-tint px-[14px] py-4">
            <Playground pairs={pairs} />
          </div>
          <div className="h-full w-1/3 shrink-0 overflow-y-auto">
            <FeedColumn side="right" posts={rightPosts} />
          </div>
        </div>
      </div>

      <div className="sticky bottom-0 grid grid-cols-3 border-t border-line bg-white">
        {TABS.map((t, i) => {
          const config = TAB_CONFIG[t];
          const active = i === index;
          return (
            <button
              key={t}
              type="button"
              onClick={() => setIndex(i)}
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
