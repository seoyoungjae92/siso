"use client";

import { useState, useTransition } from "react";

import { postComment } from "@/app/pairs/[id]/actions";

declare global {
  interface Window {
    grecaptcha?: {
      enterprise: {
        ready: (callback: () => void) => void;
        execute: (siteKey: string, options: { action: string }) => Promise<string>;
      };
    };
  }
}

const BODY_MAX_LENGTH = 2000;

// 사이트 키 미설정(reCAPTCHA 계정 발급 전)이면 undefined — 이때는 스크립트
// 자체가 로드 안 되므로(layout.tsx) 토큰 없이 그대로 제출한다.
const RECAPTCHA_SITE_KEY = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY;

function getRecaptchaToken(): Promise<string | undefined> {
  if (!RECAPTCHA_SITE_KEY || !window.grecaptcha) {
    return Promise.resolve(undefined);
  }
  return new Promise((resolve) => {
    window.grecaptcha!.enterprise.ready(() => {
      window.grecaptcha!.enterprise.execute(RECAPTCHA_SITE_KEY, { action: "comment" }).then(resolve);
    });
  });
}

const STANCE_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "선택 안 함" },
  { value: "left", label: "좌" },
  { value: "neutral", label: "중립" },
  { value: "right", label: "우" },
];

// VoteWidget과 동일한 색 언어(좌=파랑/중립=보라/우=빨강, 평소엔 옅은 톤,
// 선택 시 꽉 찬 색)를 여기도 재사용 — 브라우저 기본 라디오 버튼이 화면
// 전체 톤과 안 어울려서 촌스러워 보인다는 피드백으로 교체(2026-09-03).
const STANCE_STYLE: Record<string, { idle: string; active: string }> = {
  "": {
    idle: "border-line text-[#767268]",
    active: "border-line bg-line text-ink",
  },
  left: {
    idle: "border-left-blue/25 text-left-blue",
    active: "border-left-blue bg-left-blue text-white",
  },
  neutral: {
    idle: "border-playground/25 text-playground",
    active: "border-playground bg-playground text-white",
  },
  right: {
    idle: "border-right-red/25 text-right-red",
    active: "border-right-red bg-right-red text-white",
  },
};

export function CommentForm({
  pairId,
  parentId,
  onSuccess,
}: {
  pairId: string;
  parentId?: number;
  onSuccess?: () => void;
}) {
  const [body, setBody] = useState("");
  const [stance, setStance] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    startTransition(async () => {
      const recaptchaToken = await getRecaptchaToken();
      const result = await postComment(pairId, body, parentId, stance || undefined, recaptchaToken);
      if (!result.ok) {
        setError(result.error ?? "오류가 발생했습니다.");
        return;
      }
      setBody("");
      setStance("");
      onSuccess?.();
    });
  }

  return (
    <form onSubmit={handleSubmit} className="mb-3">
      {!parentId && (
        <p className="mb-1.5 text-[11px] text-[#767268]">
          욕설·비속어, 인신공격성 표현은 작성이 제한되며 신고 누적 시 운영자
          검토 후 가려질 수 있습니다.
        </p>
      )}
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={parentId ? "답글을 입력하세요" : "댓글을 입력하세요"}
        rows={parentId ? 2 : 3}
        maxLength={BODY_MAX_LENGTH}
        className="w-full rounded-[10px] border border-line p-2.5 text-sm"
      />
      <p className="mt-1 text-right text-[11px] text-[#767268]">
        {body.length}/{BODY_MAX_LENGTH}
      </p>
      <div className="mt-1.5 flex items-center justify-between">
        {!parentId && (
          <div className="flex gap-1.5" role="radiogroup" aria-label="입장 태그 선택">
            {STANCE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                role="radio"
                aria-checked={stance === option.value}
                onClick={() => setStance(option.value)}
                className={`rounded-full border px-2.5 py-1 text-[11px] font-bold transition-colors ${
                  stance === option.value
                    ? STANCE_STYLE[option.value].active
                    : STANCE_STYLE[option.value].idle
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        )}
        <button
          type="submit"
          disabled={isPending}
          className="ml-auto rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
        >
          {isPending ? "작성 중..." : "작성"}
        </button>
      </div>
      {error && <p className="mt-1 text-xs text-right-red">{error}</p>}
      {/* 배지를 CSS로 숨긴 대신(globals.css) 구글 정책상 필요한 고지 문구 —
          사이트 키 없으면(reCAPTCHA 자체가 꺼져있으면) 표시 안 하고, 답글
          입력창마다 반복 노출되지 않도록 최상위 댓글 입력창에만 표시 */}
      {!parentId && RECAPTCHA_SITE_KEY && (
        <p className="mt-1.5 text-[10px] text-[#767268]">
          이 사이트는 reCAPTCHA로 보호되며 Google{" "}
          <a href="https://policies.google.com/privacy" className="underline" target="_blank" rel="noreferrer">
            개인정보처리방침
          </a>
          과{" "}
          <a href="https://policies.google.com/terms" className="underline" target="_blank" rel="noreferrer">
            서비스 약관
          </a>
          이 적용됩니다.
        </p>
      )}
    </form>
  );
}
