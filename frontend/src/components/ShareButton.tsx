"use client";

import { useState } from "react";

export function ShareButton({
  title,
  url,
  label = "🔗 공유하기",
  className = "mb-4 flex items-center gap-1.5 rounded-full border border-line bg-white px-3 py-1.5 text-xs font-bold text-[#6B6960]",
}: {
  title: string;
  url?: string;
  label?: string;
  className?: string;
}) {
  const [copied, setCopied] = useState(false);

  async function handleShare(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const shareUrl = url ?? window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title, url: shareUrl });
      } catch {
        // 사용자가 공유 시트를 취소한 경우 등 — 별도 처리 불필요
      }
      return;
    }

    await navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <button type="button" onClick={handleShare} className={className}>
      {copied ? "복사됨" : label}
    </button>
  );
}
