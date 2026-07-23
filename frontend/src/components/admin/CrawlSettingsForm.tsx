"use client";

import { useState, useTransition } from "react";

import { postUpdateCrawlSettings, type CrawlSettingsInput } from "@/app/admin/settings/actions";
import type { CrawlSettings } from "@/lib/admin";

const FIELDS: {
  key: keyof CrawlSettingsInput;
  label: string;
  step: string;
  hint: string;
}[] = [
  {
    key: "matchSimilarityThreshold",
    label: "매칭 유사도 임계값",
    step: "0.01",
    hint: "좌우 글을 같은 주제로 묶는 코사인 유사도 기준(0~1)",
  },
  {
    key: "pruneSimilarityThreshold",
    label: "정리(prune) 유사도 임계값",
    step: "0.01",
    hint: "\"같은 주제\"로 볼 다른 글을 셀 때 쓰는 유사도 기준(0~1)",
  },
  {
    key: "minClusterSize",
    label: "최소 클러스터 크기",
    step: "1",
    hint: "이 개수(자기 포함) 이상 모여야 \"핫\"으로 간주",
  },
  {
    key: "gracePeriodHours",
    label: "유예 기간(시간)",
    step: "1",
    hint: "수집 후 이 시간이 지나야 정리 대상 여부를 판단",
  },
  {
    key: "displayWindowDays",
    label: "노출 기간(일)",
    step: "1",
    hint: "피드·플레이그라운드에 보여줄 최근 기간",
  },
];

export function CrawlSettingsForm({ initial }: { initial: CrawlSettings }) {
  const [values, setValues] = useState<CrawlSettingsInput>({
    matchSimilarityThreshold: initial.matchSimilarityThreshold,
    pruneSimilarityThreshold: initial.pruneSimilarityThreshold,
    minClusterSize: initial.minClusterSize,
    gracePeriodHours: initial.gracePeriodHours,
    displayWindowDays: initial.displayWindowDays,
  });
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSavedAt(null);
    startTransition(async () => {
      const result = await postUpdateCrawlSettings(values);
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
      {FIELDS.map((field) => (
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
