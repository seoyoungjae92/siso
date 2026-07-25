"use client";

import { useState, useTransition } from "react";

import { postResolveFeedback } from "@/app/admin/feedback/actions";

export function ResolveFeedbackButton({ id }: { id: number }) {
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function run() {
    setError(null);
    startTransition(async () => {
      const result = await postResolveFeedback(id);
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
        onClick={run}
        className="rounded-full border border-line px-3.5 py-1.5 text-xs font-bold text-[#6B6960] disabled:opacity-50"
      >
        처리완료로 표시
      </button>
      {error && <span className="text-[11px] text-right-red">{error}</span>}
    </div>
  );
}
