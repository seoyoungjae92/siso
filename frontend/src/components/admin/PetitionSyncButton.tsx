"use client";

import { useState, useTransition } from "react";

import { postSyncPetitions } from "@/app/admin/petitions/actions";

export function PetitionSyncButton() {
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function run() {
    setError(null);
    setMessage(null);
    startTransition(async () => {
      const result = await postSyncPetitions();
      if (!result.ok) {
        setError(result.error ?? "동기화에 실패했습니다.");
        return;
      }
      setMessage(`갱신 ${result.upserted}건 · 마감 ${result.closed}건`);
    });
  }

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={isPending}
        onClick={run}
        className="rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
      >
        {isPending ? "동기화 중..." : "지금 동기화"}
      </button>
      {message && <span className="text-xs text-[#8A877E]">{message}</span>}
      {error && <span className="text-xs text-right-red">{error}</span>}
    </div>
  );
}
