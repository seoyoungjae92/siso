import type { Metadata } from "next";

import { BackLink } from "@/components/BackLink";
import { FeedbackForm } from "@/components/FeedbackForm";

const appName = "시소";

export const metadata: Metadata = {
  title: `제보·건의하기 - ${appName}`,
  // 폼만 있는 얇은 페이지라 검색 색인에서 제외(애드센스 저가치 콘텐츠 판정 대응).
  robots: { index: false },
};

export default function FeedbackPage() {
  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-10 sm:py-14">
      <BackLink />
      <h1 className="mb-2 text-[30px] font-extrabold leading-tight tracking-tight sm:text-[36px]">
        제보·건의하기
      </h1>
      <p className="mb-8 text-[15.5px] leading-relaxed text-[#4A4842]">
        서비스에 대한 건의, 제보, 버그 신고 등을 익명으로 남겨주세요.
      </p>
      <FeedbackForm />
    </div>
  );
}
