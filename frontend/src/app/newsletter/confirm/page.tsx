import type { Metadata } from "next";

import { postConfirmNewsletter } from "@/app/newsletter/actions";
import { BackLink } from "@/components/BackLink";

const appName = process.env.APP_NAME ?? "시소";

export const metadata: Metadata = {
  title: `뉴스레터 구독 확인 - ${appName}`,
};

export default async function NewsletterConfirmPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  const result = token ? await postConfirmNewsletter(token) : { ok: false, error: "잘못된 링크입니다." };

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <BackLink />
      <h1 className="mb-4 text-2xl font-extrabold tracking-tight">뉴스레터 구독 확인</h1>
      {result.ok ? (
        <p className="text-sm text-ink">구독이 완료됐어요! 매주 좌우 리포트를 보내드릴게요.</p>
      ) : (
        <p className="text-sm text-right-red">{result.error}</p>
      )}
    </div>
  );
}
