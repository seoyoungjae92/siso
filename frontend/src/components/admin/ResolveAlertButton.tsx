"use client";

import { useState, useTransition } from "react";

import { postResolveAlert } from "@/app/admin/abuse/actions";

export function ResolveAlertButton({ alertId }: { alertId: number }) {
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function run() {
    startTransition(async () => {
      setError(null);
      const result = await postResolveAlert(alertId);
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
        className="rounded-full bg-playground px-3.5 py-1.5 text-xs font-bold text-white disabled:opacity-50"
      >
        해결 처리
      </button>
      {error && <span className="text-[11px] text-right-red">{error}</span>}
    </div>
  );
}
