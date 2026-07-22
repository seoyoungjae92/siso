"use client";

import { useState, useTransition } from "react";

import { postVote } from "@/app/pairs/[id]/actions";
import type { TopicPairDetail } from "@/lib/comments";

type Stance = "left" | "right" | "neutral";

const STANCE_CONFIG: { value: Stance; label: string }[] = [
  { value: "left", label: "좌에 공감" },
  { value: "neutral", label: "중립" },
  { value: "right", label: "우에 공감" },
];

export function VoteWidget({ pairId, pair }: { pairId: string; pair: TopicPairDetail }) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const total = pair.leftVotes + pair.rightVotes + pair.neutralVotes;

  const leftPct = total === 0 ? 0 : Math.round((pair.leftVotes / total) * 100);
  const neutralPct = total === 0 ? 0 : Math.round((pair.neutralVotes / total) * 100);
  const rightPct = total === 0 ? 0 : 100 - leftPct - neutralPct;

  function handleVote(stance: Stance) {
    setError(null);
    startTransition(async () => {
      const result = await postVote(pairId, stance);
      if (!result.ok) {
        setError(result.error ?? "투표 처리에 실패했습니다.");
      }
    });
  }

  return (
    <div className="mb-4 rounded-[10px] border border-line bg-white p-3">
      <div className="mb-2 flex gap-2">
        {STANCE_CONFIG.map((option) => (
          <button
            key={option.value}
            type="button"
            disabled={isPending}
            onClick={() => handleVote(option.value)}
            className={`flex-1 rounded-full border px-2 py-1.5 text-xs font-bold disabled:opacity-50 ${
              pair.myStance === option.value
                ? "border-playground bg-pg-tint text-playground"
                : "border-line text-[#8A877E]"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>

      {error && <p className="mb-1.5 text-[11px] text-right-red">{error}</p>}

      <div className="mb-1.5 flex h-2.5 overflow-hidden rounded-full border border-line">
        <span className="block h-full bg-left-blue" style={{ width: `${leftPct}%` }} />
        <span className="block h-full bg-playground" style={{ width: `${neutralPct}%` }} />
        <span className="block h-full bg-right-red" style={{ width: `${rightPct}%` }} />
      </div>
      <div className="flex justify-between text-[11px] text-[#8A877E]">
        <span>좌에 공감 {leftPct}%</span>
        <span>중립 {neutralPct}%</span>
        <span>우에 공감 {rightPct}%</span>
      </div>
    </div>
  );
}
