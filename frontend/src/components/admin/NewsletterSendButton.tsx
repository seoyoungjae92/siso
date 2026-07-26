"use client";

import { useState, useTransition } from "react";

import { postSendNewsletterNow } from "@/app/admin/newsletter/actions";

export function NewsletterSendButton() {
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  function run() {
    setError(null);
    setMessage(null);
    startTransition(async () => {
      const result = await postSendNewsletterNow();
      if (!result.ok) {
        setError(result.error ?? "발송에 실패했습니다.");
        return;
      }
      setMessage(`${result.sent}명에게 발송했습니다.`);
    });
  }

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={isPending}
        onClick={run}
        className="rounded-full bg-playground px-4 py-1.5 text-xs font-bold text-white disabled:opacity-50"
      >
        {isPending ? "발송 중..." : "지금 발송"}
      </button>
      {message && <span className="text-xs text-[#8A877E]">{message}</span>}
      {error && <span className="text-xs text-right-red">{error}</span>}
    </div>
  );
}
