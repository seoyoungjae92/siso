import type { Metadata } from "next";

import { postUnsubscribeNewsletter } from "@/app/newsletter/actions";
import { BackLink } from "@/components/BackLink";

const appName = process.env.APP_NAME ?? "시소";

export const metadata: Metadata = {
  title: `뉴스레터 수신거부 - ${appName}`,
};

export default async function NewsletterUnsubscribePage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  const result = token ? await postUnsubscribeNewsletter(token) : { ok: false, error: "잘못된 링크입니다." };

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <BackLink />
      <h1 className="mb-4 text-2xl font-extrabold tracking-tight">뉴스레터 수신거부</h1>
      {result.ok ? (
        <p className="text-sm text-ink">수신거부가 처리됐어요. 더 이상 메일이 발송되지 않습니다.</p>
      ) : (
        <p className="text-sm text-right-red">{result.error}</p>
      )}
    </div>
  );
}
