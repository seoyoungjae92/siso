"use client";

import { useState, useTransition } from "react";

import { postSubscribeNewsletter } from "@/app/newsletter/actions";

export function NewsletterForm() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    startTransition(async () => {
      const result = await postSubscribeNewsletter(email);
      if (!result.ok) {
        setError(result.error ?? "구독 신청에 실패했습니다.");
        return;
      }
      setSubmitted(true);
    });
  }

  if (submitted) {
    return (
      <p className="text-[11px] text-[#767268]">
        확인 메일을 보냈어요. 메일함에서 링크를 눌러 구독을 완료해주세요.
      </p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-center gap-2">
      <span className="text-[11px] font-semibold text-[#6B6960]">주간 좌우 리포트 구독</span>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        placeholder="이메일 주소"
        className="rounded border border-line px-2 py-1 text-[11px]"
      />
      <button
        type="submit"
        disabled={isPending}
        className="rounded-full bg-playground px-3 py-1 text-[11px] font-bold text-white disabled:opacity-50"
      >
        {isPending ? "신청 중..." : "구독하기"}
      </button>
      {error && <span className="text-[11px] text-right-red">{error}</span>}
    </form>
  );
}
