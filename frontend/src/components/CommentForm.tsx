"use client";

import { useState, useTransition } from "react";

import { postComment } from "@/app/pairs/[id]/actions";

declare global {
  interface Window {
    grecaptcha?: {
      ready: (callback: () => void) => void;
      execute: (siteKey: string, options: { action: string }) => Promise<string>;
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
    window.grecaptcha!.ready(() => {
      window.grecaptcha!.execute(RECAPTCHA_SITE_KEY, { action: "comment" }).then(resolve);
    });
  });
}

const STANCE_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "선택 안 함" },
  { value: "left", label: "좌" },
  { value: "neutral", label: "중립" },
  { value: "right", label: "우" },
];

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
          <div className="flex gap-2 text-xs">
            {STANCE_OPTIONS.map((option) => (
              <label key={option.value} className="flex items-center gap-1">
                <input
                  type="radio"
                  name={`stance-${parentId ?? "top"}`}
                  value={option.value}
                  checked={stance === option.value}
                  onChange={() => setStance(option.value)}
                />
                {option.label}
              </label>
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
    </form>
  );
}
