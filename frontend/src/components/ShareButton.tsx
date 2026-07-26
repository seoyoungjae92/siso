"use client";

import { useState } from "react";

export function ShareButton({ title }: { title: string }) {
  const [copied, setCopied] = useState(false);

  async function handleShare() {
    const url = window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title, url });
      } catch {
        // 사용자가 공유 시트를 취소한 경우 등 — 별도 처리 불필요
      }
      return;
    }

    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <button
      type="button"
      onClick={handleShare}
      className="mb-4 flex items-center gap-1.5 rounded-full border border-line bg-white px-3 py-1.5 text-xs font-bold text-[#6B6960]"
    >
      {copied ? "링크가 복사되었습니다" : "🔗 공유하기"}
    </button>
  );
}
