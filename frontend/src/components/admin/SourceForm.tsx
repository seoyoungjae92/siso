"use client";

import { useState, useTransition } from "react";

import type { SourceInput } from "@/app/admin/sources/actions";

export function SourceForm({
  initial,
  submitLabel,
  onSubmit,
  onSuccess,
  onCancel,
}: {
  initial?: SourceInput;
  submitLabel: string;
  onSubmit: (input: SourceInput) => Promise<{ ok: boolean; error?: string }>;
  onSuccess?: () => void;
  onCancel?: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [side, setSide] = useState(initial?.side ?? "left");
  const [baseUrl, setBaseUrl] = useState(initial?.baseUrl ?? "");
  const [feedUrl, setFeedUrl] = useState(initial?.feedUrl ?? "");
  const [crawlType, setCrawlType] = useState(initial?.crawlType ?? "rss");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    startTransition(async () => {
      const result = await onSubmit({ name, side, baseUrl, feedUrl, crawlType });
      if (!result.ok) {
        setError(result.error ?? "오류가 발생했습니다.");
        return;
      }
      onSuccess?.();
    });
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-2 rounded-xl border border-line bg-white p-3"
    >
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="이름 (예: 루리웹)"
        required
        className="rounded border border-line px-2 py-1.5 text-sm"
      />
      <div className="flex gap-2">
        <select
          value={side}
          onChange={(e) => setSide(e.target.value)}
          className="rounded border border-line px-2 py-1.5 text-sm"
        >
          <option value="left">좌</option>
          <option value="right">우</option>
        </select>
        <select
          value={crawlType}
          onChange={(e) => setCrawlType(e.target.value)}
          className="rounded border border-line px-2 py-1.5 text-sm"
        >
          <option value="rss">RSS</option>
          <option value="html">HTML</option>
        </select>
      </div>
      <input
        value={baseUrl}
        onChange={(e) => setBaseUrl(e.target.value)}
        placeholder="기본 URL"
        required
        className="rounded border border-line px-2 py-1.5 text-sm"
      />
      <input
        value={feedUrl}
        onChange={(e) => setFeedUrl(e.target.value)}
        placeholder="피드 URL (선택)"
        className="rounded border border-line px-2 py-1.5 text-sm"
      />
      <div className="flex items-center gap-2">
        <button
          type="submit"
          disabled={isPending}
          className="rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
        >
          {isPending ? "저장 중..." : submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="text-xs font-bold text-[#8A877E]">
            취소
          </button>
        )}
        {error && <span className="text-xs text-right-red">{error}</span>}
      </div>
    </form>
  );
}
