"use client";

import { useState, useTransition } from "react";

import { postVote } from "@/app/pairs/[id]/actions";
import type { TopicPairDetail } from "@/lib/comments";
import { calculateVotePercentages } from "@/lib/pairs";

type Stance = "left" | "right" | "neutral";

const STANCE_CONFIG: { value: Stance; label: string }[] = [
  { value: "left", label: "좌에 공감" },
  { value: "neutral", label: "중립" },
  { value: "right", label: "우에 공감" },
];

// 투표 전에도 세 버튼이 다 똑같은 회색이라 구분이 안 됐음 — 사이트 전반의
// 좌(파랑)/중립(보라)/우(빨강) 색 언어를 그대로 가져와 평소에도 옅은
// 톤으로, 선택했을 때는 꽉 찬 색으로 표시한다.
const STANCE_STYLE: Record<Stance, { idle: string; active: string }> = {
  left: {
    idle: "border-left-blue/25 bg-blue-tint text-left-blue",
    active: "border-left-blue bg-left-blue text-white",
  },
  neutral: {
    idle: "border-playground/25 bg-pg-tint text-playground",
    active: "border-playground bg-playground text-white",
  },
  right: {
    idle: "border-right-red/25 bg-red-tint text-right-red",
    active: "border-right-red bg-right-red text-white",
  },
};

export function VoteWidget({ pairId, pair }: { pairId: string; pair: TopicPairDetail }) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const { leftPct, neutralPct, rightPct } = calculateVotePercentages(pair);

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
                ? STANCE_STYLE[option.value].active
                : STANCE_STYLE[option.value].idle
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
