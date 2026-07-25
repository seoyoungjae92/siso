"use client";

import { useEffect, useRef, useState } from "react";

const SHOW_THRESHOLD_PX = 400;

function scrollTopOf(target: EventTarget | null): number {
  if (target === document) {
    return window.scrollY;
  }
  return (target as Element)?.scrollTop ?? 0;
}

export function BackToTopButton() {
  const [visible, setVisible] = useState(false);
  // 데스크톱은 window 전체가 스크롤되지만, 모바일 탭(MobileTabs)은 각
  // 패널 내부의 overflow-y-auto가 스크롤된다 — 어느 쪽이든 마지막으로
  // 스크롤된 대상을 기억해뒀다가 그 대상만 맨 위로 되돌린다. scroll
  // 이벤트는 버블링되지 않지만 capture 단계에서는 document까지 올라오는
  // 길목에서 잡히므로, 중첩 스크롤 컨테이너까지 document 리스너 하나로
  // 감지할 수 있다.
  const scrollTargetRef = useRef<Window | Element | null>(null);

  useEffect(() => {
    scrollTargetRef.current = window;

    function onScroll(e: Event) {
      const isWindowScroll = e.target === document;
      scrollTargetRef.current = isWindowScroll ? window : (e.target as Element);
      setVisible(scrollTopOf(e.target) > SHOW_THRESHOLD_PX);
    }

    document.addEventListener("scroll", onScroll, { capture: true, passive: true });
    return () => document.removeEventListener("scroll", onScroll, true);
  }, []);

  if (!visible) {
    return null;
  }

  return (
    <button
      type="button"
      onClick={() => scrollTargetRef.current?.scrollTo({ top: 0, behavior: "smooth" })}
      aria-label="맨 위로"
      className="fixed bottom-20 right-4 z-40 flex h-11 w-11 items-center justify-center rounded-full bg-playground text-lg text-white shadow-[0_4px_14px_rgba(110,61,116,.35)] lg:bottom-6"
    >
      ↑
    </button>
  );
}
