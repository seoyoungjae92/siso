"use client";

import { useState, useTransition } from "react";

import { postModerate } from "@/app/admin/reports/actions";

export function ModerateButtons({ commentId }: { commentId: number }) {
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function run(action: "blind" | "dismiss") {
    startTransition(async () => {
      setError(null);
      const result = await postModerate(commentId, action);
      if (!result.ok) {
        setError(result.error ?? "처리에 실패했습니다.");
      }
    });
  }

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={isPending}
        onClick={() => run("blind")}
        className="rounded-full bg-right-red px-3.5 py-1.5 text-xs font-bold text-white disabled:opacity-50"
      >
        블라인드
      </button>
      <button
        type="button"
        disabled={isPending}
        onClick={() => run("dismiss")}
        className="rounded-full border border-line px-3.5 py-1.5 text-xs font-bold text-[#6B6960] disabled:opacity-50"
      >
        반려
      </button>
      {error && <span className="text-[11px] text-right-red">{error}</span>}
    </div>
  );
}
