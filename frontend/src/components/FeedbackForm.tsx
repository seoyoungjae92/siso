"use client";

import { useState, useTransition } from "react";

import { postFeedback } from "@/app/feedback/actions";

const CATEGORIES: { value: string; label: string }[] = [
  { value: "suggestion", label: "건의" },
  { value: "report", label: "제보" },
  { value: "bug", label: "버그 신고" },
  { value: "etc", label: "기타" },
];

export function FeedbackForm() {
  const [category, setCategory] = useState("suggestion");
  const [body, setBody] = useState("");
  const [contact, setContact] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    startTransition(async () => {
      const result = await postFeedback(category, body, contact);
      if (!result.ok) {
        setError(result.error ?? "제출에 실패했습니다.");
        return;
      }
      setSubmitted(true);
      setBody("");
      setContact("");
    });
  }

  if (submitted) {
    return (
      <div className="rounded-xl border border-line bg-white p-6 text-center">
        <p className="text-[15px] font-bold text-ink">전달됐습니다. 감사합니다!</p>
        <p className="mt-1 text-[13px] text-[#767268]">남겨주신 내용은 운영자가 확인합니다.</p>
        <button
          type="button"
          onClick={() => setSubmitted(false)}
          className="mt-4 text-[13px] font-semibold text-playground hover:underline"
        >
          하나 더 보내기
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-xl border border-line bg-white p-4">
      <div className="flex gap-2">
        {CATEGORIES.map((c) => (
          <button
            key={c.value}
            type="button"
            onClick={() => setCategory(c.value)}
            className={`rounded-full px-3.5 py-1.5 text-xs font-bold ${
              category === c.value ? "bg-playground text-white" : "border border-line text-[#6B6960]"
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">내용</span>
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          required
          rows={6}
          placeholder="건의사항, 제보, 버그 등 자유롭게 남겨주세요."
          className="rounded border border-line px-3 py-2 text-sm"
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">이메일 (선택)</span>
        <input
          type="email"
          value={contact}
          onChange={(e) => setContact(e.target.value)}
          placeholder="답변받고 싶으면 남겨주세요"
          className="rounded border border-line px-3 py-2 text-sm"
        />
      </label>

      <div className="flex items-center gap-2">
        <button
          type="submit"
          disabled={isPending}
          className="rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
        >
          {isPending ? "제출 중..." : "제출"}
        </button>
        {error && <span className="text-xs text-right-red">{error}</span>}
      </div>
    </form>
  );
}
