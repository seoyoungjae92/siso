"use client";

import { useState, useTransition } from "react";

import { postUpdateElectionSettings } from "@/app/admin/settings/actions";
import type { ElectionSettings } from "@/lib/admin";

export function ElectionSettingsForm({ initial }: { initial: ElectionSettings }) {
  const [enabled, setEnabled] = useState(initial.enabled);
  const [threshold, setThreshold] = useState(initial.overrideAutoBlindThreshold);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSavedAt(null);
    startTransition(async () => {
      const result = await postUpdateElectionSettings({
        enabled,
        overrideAutoBlindThreshold: threshold,
      });
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
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={enabled}
          onChange={(e) => setEnabled(e.target.checked)}
          className="h-4 w-4"
        />
        <span className="text-sm font-bold">선거 모드 켜기</span>
      </label>
      <p className="text-xs text-[#8A877E]">
        켜면 사이트 전체에서 투표 버튼/비율 그래프가 숨겨집니다(댓글은 그대로 보임). 공직선거법상
        선거일 전 여론조사 공표 금지 리스크를 피하기 위한 안전장치입니다.
      </p>
      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">선거 모드 중 자동 블라인드 임계값</span>
        <input
          type="number"
          step="1"
          value={threshold}
          onChange={(e) => setThreshold(Number(e.target.value))}
          required
          className="rounded border border-line px-2 py-1.5 text-sm"
        />
        <span className="text-xs text-[#8A877E]">
          선거 모드가 켜져 있는 동안, 평소 설정한 값보다 이 값이 낮으면 이 값을 대신 적용(더
          엄격하게)합니다. 꺼지면 평소 설정으로 자동 복귀합니다.
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
