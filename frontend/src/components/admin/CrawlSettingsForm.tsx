"use client";

import { useState, useTransition } from "react";

import { postUpdateCrawlSettings, type CrawlSettingsInput } from "@/app/admin/settings/actions";
import type { CrawlSettings } from "@/lib/admin";

const NUMBER_FIELDS: {
  key: Exclude<keyof CrawlSettingsInput, "synthesisModel">;
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
  {
    key: "synthesisLimit",
    label: "주제 합성 사이클당 처리 개수",
    step: "1",
    hint: "크롤 사이클 1회당 AI로 합성할 플레이그라운드 주제 최대 개수",
  },
  {
    key: "deadLinkScanLimit",
    label: "데드링크 확인 사이클당 처리 개수",
    step: "1",
    hint: "매칭 안 된 글이 쌓이면 도메인당 10초 간격 정책 때문에 한 사이클이 무한정 길어질 수 있어 상한을 둠",
  },
  {
    key: "pruneScanLimit",
    label: "정리(prune) 사이클당 처리 개수",
    step: "1",
    hint: "매칭 안 된 글이 쌓이면 정리 후보도 같이 늘어나 한 사이클이 길어질 수 있어 상한을 둠",
  },
  {
    key: "sourceFailureThreshold",
    label: "소스 자동 비활성화 임계값(연속 실패 횟수)",
    step: "1",
    hint: "소스가 이 횟수만큼 연속으로 수집 실패하면 자동으로 비활성화됨",
  },
  {
    key: "cohortSimilarityThreshold",
    label: "코호트 결속 유사도 임계값",
    step: "0.01",
    hint: "같은 진영 내에서 \"같은 이야기\"로 묶어 요약에 같이 반영할 글을 판단하는 유사도 기준(0~1)",
  },
  {
    key: "synthesisMinPostsPerSide",
    label: "주제 합성 최소 글 개수(진영당)",
    step: "1",
    hint: "1=지금과 동일(변경 없음). 올리면 그 순간부터 좌·우 각각 이 개수 이상 모여야 새 주제가 뜸",
  },
];

export function CrawlSettingsForm({ initial }: { initial: CrawlSettings }) {
  const [values, setValues] = useState<CrawlSettingsInput>({
    matchSimilarityThreshold: initial.matchSimilarityThreshold,
    pruneSimilarityThreshold: initial.pruneSimilarityThreshold,
    minClusterSize: initial.minClusterSize,
    gracePeriodHours: initial.gracePeriodHours,
    displayWindowDays: initial.displayWindowDays,
    synthesisLimit: initial.synthesisLimit,
    synthesisModel: initial.synthesisModel,
    deadLinkScanLimit: initial.deadLinkScanLimit,
    pruneScanLimit: initial.pruneScanLimit,
    sourceFailureThreshold: initial.sourceFailureThreshold,
    cohortSimilarityThreshold: initial.cohortSimilarityThreshold,
    synthesisMinPostsPerSide: initial.synthesisMinPostsPerSide,
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
      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">주제 합성 모델</span>
        <input
          type="text"
          value={values.synthesisModel}
          onChange={(e) => setValues((prev) => ({ ...prev, synthesisModel: e.target.value }))}
          required
          className="rounded border border-line px-2 py-1.5 text-sm"
        />
        <span className="text-xs text-[#8A877E]">
          OpenRouter 모델 슬러그(예: openrouter/free, anthropic/claude-haiku-4.5)
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
