"use client";

import { useState, useTransition } from "react";

import { postUpdateModerationSettings } from "@/app/admin/settings/actions";
import type { ModerationSettings } from "@/lib/admin";

export function ModerationSettingsForm({ initial }: { initial: ModerationSettings }) {
  const [threshold, setThreshold] = useState(initial.autoBlindReportThreshold);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSavedAt(null);
    startTransition(async () => {
      const result = await postUpdateModerationSettings({ autoBlindReportThreshold: threshold });
      if (!result.ok) {
        setError(result.error ?? "오류가 발생했습니다.");
        return;
      }
      setSavedAt(new Date().toLocaleTimeString("ko-KR"));
    });
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-4 rounded-xl border border-line bg-white p-4"
    >
      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">자동 블라인드 신고 임계값</span>
        <input
          type="number"
          step="1"
          value={threshold}
          onChange={(e) => setThreshold(Number(e.target.value))}
          required
          className="rounded border border-line px-2 py-1.5 text-sm"
        />
        <span className="text-xs text-[#8A877E]">
          댓글 하나에 이 건수만큼 신고가 누적되면 자동으로 블라인드 처리
        </span>
      </label>
      <div className="flex items-center gap-2">
        <button
          type="submit"
          disabled={isPending}
          className="rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
        >
          {isPending ? "저장 중..." : "저장"}
        </button>
        {savedAt && <span className="text-xs text-[#8A877E]">{savedAt} 저장됨</span>}
        {error && <span className="text-xs text-right-red">{error}</span>}
      </div>
    </form>
  );
}
