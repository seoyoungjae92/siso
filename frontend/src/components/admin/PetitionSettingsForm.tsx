"use client";

import { useState, useTransition } from "react";

import { postUpdatePetitionSettings, type PetitionSettingsInput } from "@/app/admin/settings/actions";
import type { PetitionSettings } from "@/lib/admin";

const NUMBER_FIELDS: {
  key: Exclude<keyof PetitionSettingsInput, "eraco">;
  label: string;
  step: string;
  hint: string;
}[] = [
  {
    key: "topN",
    label: "노출 개수",
    step: "1",
    hint: "위젯에 보여줄 상위 청원 개수",
  },
  {
    key: "windowDays",
    label: "집계 기간(일)",
    step: "1",
    hint: "접수일 기준 이 기간 이내 청원만 집계(국민동의청원 동의 수집 기간과 동일하게 30일 권장)",
  },
  {
    key: "cacheTtlMinutes",
    label: "캐시 유지 시간(분)",
    step: "1",
    hint: "국회 Open API 응답을 이 시간만큼 캐싱",
  },
];

export function PetitionSettingsForm({ initial }: { initial: PetitionSettings }) {
  const [values, setValues] = useState<PetitionSettingsInput>({
    eraco: initial.eraco,
    topN: initial.topN,
    windowDays: initial.windowDays,
    cacheTtlMinutes: initial.cacheTtlMinutes,
  });
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSavedAt(null);
    startTransition(async () => {
      const result = await postUpdatePetitionSettings(values);
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
        <span className="text-sm font-bold">대수</span>
        <input
          type="text"
          value={values.eraco}
          onChange={(e) => setValues((prev) => ({ ...prev, eraco: e.target.value }))}
          required
          className="rounded border border-line px-2 py-1.5 text-sm"
        />
        <span className="text-xs text-[#8A877E]">
          국회 Open API ERACO 파라미터 값(예: 제22대) — 새 국회 임기가 시작되면 갱신
        </span>
      </label>
      {NUMBER_FIELDS.map((field) => (
        <label key={field.key} className="flex flex-col gap-1">
          <span className="text-sm font-bold">{field.label}</span>
          <input
            type="number"
            step={field.step}
            value={values[field.key]}
            onChange={(e) =>
              setValues((prev) => ({ ...prev, [field.key]: Number(e.target.value) }))
            }
            required
            className="rounded border border-line px-2 py-1.5 text-sm"
          />
          <span className="text-xs text-[#8A877E]">{field.hint}</span>
        </label>
      ))}
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
