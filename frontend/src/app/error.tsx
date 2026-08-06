"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

export default function ErrorPage({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
    Sentry.captureException(error);
  }, [error]);

  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center px-4 py-20 text-center">
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">일시적인 오류가 발생했습니다</h1>
      <p className="mb-6 text-sm text-[#6B6960]">
        잠시 후 다시 시도해주세요. 문제가 계속되면 새로고침해보세요.
      </p>
      <button
        type="button"
        onClick={() => unstable_retry()}
        className="rounded-full bg-playground px-5 py-2 text-sm font-bold text-white"
      >
        다시 시도
      </button>
    </div>
  );
}
