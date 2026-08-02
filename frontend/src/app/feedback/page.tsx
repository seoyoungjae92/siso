import type { Metadata } from "next";

import { BackLink } from "@/components/BackLink";
import { FeedbackForm } from "@/components/FeedbackForm";

const appName = "시소";

export const metadata: Metadata = {
  title: `제보·건의하기 - ${appName}`,
};

export default function FeedbackPage() {
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <BackLink />
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">제보·건의하기</h1>
      <p className="mb-8 text-sm text-[#767268]">
        서비스에 대한 건의, 제보, 버그 신고 등을 익명으로 남겨주세요.
      </p>
      <FeedbackForm />
    </div>
  );
}
