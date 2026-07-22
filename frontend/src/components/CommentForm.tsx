"use client";

import { useState, useTransition } from "react";

import { postComment } from "@/app/pairs/[id]/actions";

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
      const result = await postComment(pairId, body, parentId, stance || undefined);
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
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={parentId ? "답글을 입력하세요" : "댓글을 입력하세요"}
        rows={parentId ? 2 : 3}
        className="w-full rounded-[10px] border border-line p-2.5 text-sm"
      />
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
