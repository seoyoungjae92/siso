"use client";

import { useState, useTransition } from "react";

import { postToggleSource, postUpdateSource } from "@/app/admin/sources/actions";
import { SourceForm } from "@/components/admin/SourceForm";
import type { Source } from "@/lib/admin";

export function SourceRow({ source }: { source: Source }) {
  const [editing, setEditing] = useState(false);
  const [toggleError, setToggleError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  if (editing) {
    return (
      <SourceForm
        initial={{
          name: source.name,
          side: source.side,
          baseUrl: source.baseUrl,
          feedUrl: source.feedUrl ?? "",
          crawlType: source.crawlType,
        }}
        submitLabel="수정 완료"
        onSubmit={(input) => postUpdateSource(source.id, input)}
        onSuccess={() => setEditing(false)}
        onCancel={() => setEditing(false)}
      />
    );
  }

  return (
    <div className="flex items-center justify-between rounded-xl border border-line bg-white p-3">
      <div className="text-sm">
        <div className="flex items-center gap-2 font-bold">
          <span>{source.name}</span>
          <span className={source.side === "left" ? "text-left-blue" : "text-right-red"}>
            {source.side === "left" ? "좌" : "우"}
          </span>
          <span className="text-xs font-normal text-[#8A877E]">{source.crawlType.toUpperCase()}</span>
        </div>
        <div className="text-xs text-[#8A877E]">{source.baseUrl}</div>
      </div>
      <div className="flex items-center gap-2">
        {toggleError && <span className="text-xs text-right-red">{toggleError}</span>}
        <button
          type="button"
          disabled={isPending}
          onClick={() =>
            startTransition(async () => {
              setToggleError(null);
              const result = await postToggleSource(source.id);
              if (!result.ok) {
                setToggleError(result.error ?? "처리에 실패했습니다.");
              }
            })
          }
          className={`rounded-full px-3 py-1 text-xs font-bold disabled:opacity-50 ${
            source.enabled ? "bg-playground text-white" : "border border-line text-[#8A877E]"
          }`}
        >
          {source.enabled ? "활성" : "비활성"}
        </button>
        <button
          type="button"
          onClick={() => setEditing(true)}
          className="text-xs font-bold text-[#8A877E]"
        >
          수정
        </button>
      </div>
    </div>
  );
}
