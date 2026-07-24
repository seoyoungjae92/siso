"use client";

import { useState, useTransition } from "react";

import { postUpdateAbuseSettings, type AbuseSettingsInput } from "@/app/admin/settings/actions";
import type { AbuseSettings } from "@/lib/admin";

const FIELDS: {
  key: keyof AbuseSettingsInput;
  label: string;
  step: string;
  hint: string;
}[] = [
  {
    key: "multiAccountClusterSize",
    label: "다중 계정 클러스터 크기",
    step: "1",
    hint: "동일 IP에서 이 개수 이상 익명 ID가 생기면 다중 계정으로 간주",
  },
  {
    key: "multiAccountTrustPenaltyMultiplier",
    label: "다중 계정 신뢰도 페널티 배수",
    step: "0.01",
    hint: "다중 계정으로 간주된 그룹의 신뢰도 점수에 곱하는 배수(0~1)",
  },
  {
    key: "trustMaturityHours",
    label: "신뢰도 성숙 기간(시간)",
    step: "1",
    hint: "가입 후 이 시간이 지나야 신뢰도 점수가 최대치에 도달",
  },
  {
    key: "trustMinWeight",
    label: "신뢰도 최소 가중치",
    step: "0.01",
    hint: "신규 익명 ID의 초기 신뢰도 가중치(0~1)",
  },
  {
    key: "duplicateSimilarityThreshold",
    label: "중복 댓글 유사도 임계값",
    step: "0.01",
    hint: "본인의 최근 댓글과 이 유사도(0~1) 이상이면 반복 작성으로 거부",
  },
  {
    key: "duplicateLookbackCount",
    label: "중복 검사 대상 댓글 개수",
    step: "1",
    hint: "본인의 최근 댓글 몇 개까지 비교할지",
  },
  {
    key: "duplicateLookbackMinutes",
    label: "중복 검사 기간(분)",
    step: "1",
    hint: "이 시간 이내의 댓글만 중복 검사 대상",
  },
  {
    key: "spikeWindowMinutes",
    label: "급증 탐지 시간 윈도(분)",
    step: "1",
    hint: "이 시간 동안의 투표/추천 수를 세어 급증 여부 판단",
  },
  {
    key: "spikeVoteThreshold",
    label: "투표 급증 임계값",
    step: "1",
    hint: "시간 윈도 내 한 쌍에 이 개수 이상 투표가 몰리면 알림",
  },
  {
    key: "spikeReactionThreshold",
    label: "추천 급증 임계값",
    step: "1",
    hint: "시간 윈도 내 한 댓글에 이 개수 이상 추천이 몰리면 알림",
  },
];

export function AbuseSettingsForm({ initial }: { initial: AbuseSettings }) {
  const [values, setValues] = useState<AbuseSettingsInput>({
    multiAccountClusterSize: initial.multiAccountClusterSize,
    multiAccountTrustPenaltyMultiplier: initial.multiAccountTrustPenaltyMultiplier,
    trustMaturityHours: initial.trustMaturityHours,
    trustMinWeight: initial.trustMinWeight,
    duplicateSimilarityThreshold: initial.duplicateSimilarityThreshold,
    duplicateLookbackCount: initial.duplicateLookbackCount,
    duplicateLookbackMinutes: initial.duplicateLookbackMinutes,
    spikeWindowMinutes: initial.spikeWindowMinutes,
    spikeVoteThreshold: initial.spikeVoteThreshold,
    spikeReactionThreshold: initial.spikeReactionThreshold,
  });
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSavedAt(null);
    startTransition(async () => {
      const result = await postUpdateAbuseSettings(values);
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
