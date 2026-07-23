"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";

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
  leftHasMore,
  rightPosts,
  rightHasMore,
  pairs,
  pairsHasMore,
}: {
  leftPosts: PostSummary[];
  leftHasMore: boolean;
  rightPosts: PostSummary[];
  rightHasMore: boolean;
  pairs: TopicPair[];
  pairsHasMore: boolean;
}) {
  const [index, setIndex] = useState(TABS.indexOf("playground"));
  const [dragPx, setDragPx] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [offsetPx, setOffsetPx] = useState<number | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const gesture = useRef<{ startX: number; startY: number; horizontal: boolean | null } | null>(
    null,
  );

  // 각 탭 패널의 overflow-y-auto가 실제로 스크롤을 가둬야(clip) 짧은
  // 플레이그라운드 탭이 옆의 긴 좌/우 탭 길이만큼 스크롤되는 문제가
  // 없어지는데, body가 min-h-full이라 조상 어디에도 고정 높이가 없으면
  // 각 패널이 그냥 내용 그대로의 높이로 렌더링되어 overflow가 무의미해짐.
  // 그래서 이 컴포넌트 루트에만(다른 페이지에는 영향 없도록) 뷰포트
  // 기준 실측 높이를 부여한다 — 헤더 높이를 가정하지 않고 실제 렌더된
  // 위치(top)를 재서 100dvh에서 빼는 방식.
  useLayoutEffect(() => {
    function measure() {
      const el = rootRef.current;
      if (!el) return;
      setOffsetPx(el.getBoundingClientRect().top + window.scrollY);
    }
    measure();
    window.addEventListener("resize", measure);
    return () => window.removeEventListener("resize", measure);
  }, []);

  function handleTouchStart(e: React.TouchEvent) {
    const t = e.touches[0];
    gesture.current = { startX: t.clientX, startY: t.clientY, horizontal: null };
    setDragging(true);
  }

  // 대각선 스와이프 시 브라우저의 네이티브 세로 스크롤이 같이 끼어들지
  // 않도록(터치가 수평으로 판정된 이후엔 preventDefault로 완전히 끊어야
  // 함) React의 합성 이벤트가 아니라 네이티브 리스너를 passive:false로
  // 직접 붙인다 — JSX onTouchMove는 기본 passive라 preventDefault가
  // 동작하지 않는다.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    function onTouchMove(e: TouchEvent) {
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

      e.preventDefault();

      // 맨 끝 탭에서 더 당기면 저항감(러버밴드)을 주고 그 이상은 안 넘어가게
      let next = dx;
      if (index === 0 && next > 0) next *= 0.35;
      if (index === TABS.length - 1 && next < 0) next *= 0.35;
      setDragPx(next);
    }

    el.addEventListener("touchmove", onTouchMove, { passive: false });
    return () => el.removeEventListener("touchmove", onTouchMove);
  }, [index]);

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
    <div
      ref={rootRef}
      className="flex flex-col"
      style={offsetPx != null ? { height: `calc(100dvh - ${offsetPx}px)` } : undefined}
    >
      <div
        ref={containerRef}
        className="relative min-h-0 flex-1 overflow-hidden"
        style={{ touchAction: "pan-y", overscrollBehaviorX: "none" }}
        onTouchStart={handleTouchStart}
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
            <FeedColumn side="left" posts={leftPosts} hasMore={leftHasMore} />
          </div>
          <div className="h-full w-1/3 shrink-0 overflow-y-auto bg-pg-tint px-[14px] py-4">
            <Playground pairs={pairs} hasMore={pairsHasMore} />
          </div>
          <div className="h-full w-1/3 shrink-0 overflow-y-auto">
            <FeedColumn side="right" posts={rightPosts} hasMore={rightHasMore} />
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
