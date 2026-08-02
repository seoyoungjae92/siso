"use client";

import { useState } from "react";

import { ShareButton } from "@/components/ShareButton";
import type { Petition } from "@/lib/petitions";

const COLLAPSED_COUNT = 3;

const RANK_BADGE_STYLE: Record<number, string> = {
  0: "bg-playground text-white text-[13px]",
  1: "bg-playground/15 text-playground text-[12px]",
  2: "bg-playground/8 text-playground text-[12px]",
};

function RankBadge({ index }: { index: number }) {
  const style = RANK_BADGE_STYLE[index] ?? "text-[#767268] text-[12px]";
  const filled = index in RANK_BADGE_STYLE;
  return (
    <span
      className={`flex h-5 w-5 shrink-0 items-center justify-center font-extrabold ${style} ${
        filled ? "rounded-full" : ""
      }`}
    >
      {index + 1}
    </span>
  );
}

export function PetitionWidget({ petitions }: { petitions: Petition[] }) {
  const [expanded, setExpanded] = useState(false);

  if (petitions.length === 0) {
    return null;
  }

  const visible = expanded ? petitions : petitions.slice(0, COLLAPSED_COUNT);
  const hasMore = petitions.length > COLLAPSED_COUNT;

  return (
    <div className="mb-2.5 overflow-hidden rounded-xl border border-line bg-white">
      <div className="flex items-center justify-between bg-playground px-4 py-2.5 text-white">
        <b className="text-[13px] tracking-wide">🔥 실시간 청원 랭킹</b>
      </div>
      <ol className="divide-y divide-line">
        {visible.map((petition, index) => (
          <li key={petition.id} className="flex items-center gap-1 px-4 py-2.5 hover:bg-pg-tint">
            <a
              href={petition.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex min-w-0 flex-1 items-center gap-2.5"
            >
              <RankBadge index={index} />
              <span className="line-clamp-1 flex-1 text-[13px] font-medium text-ink">
                {petition.title}
              </span>
              <span className="shrink-0 text-[11px] text-[#767268]">
                {petition.agreeCount.toLocaleString("ko-KR")}명
              </span>
            </a>
            <ShareButton
              title={petition.title}
              url={petition.linkUrl}
              label="🔗"
              className="shrink-0 rounded-full p-1.5 text-xs text-[#767268] hover:bg-pg-tint"
            />
          </li>
        ))}
      </ol>
      {hasMore && (
        <button
          type="button"
          onClick={() => setExpanded((prev) => !prev)}
          className="block w-full border-t border-line py-2 text-center text-xs font-bold text-[#767268] hover:bg-pg-tint"
        >
          {expanded ? "접기 ▲" : `더보기 (${petitions.length - COLLAPSED_COUNT}) ▼`}
        </button>
      )}
    </div>
  );
}
