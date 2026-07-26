import { NewsletterSendButton } from "@/components/admin/NewsletterSendButton";
import { fetchNewsletterStats, requireAdmin } from "@/lib/admin";

export default async function AdminNewsletterPage() {
  await requireAdmin();
  const stats = await fetchNewsletterStats();

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-extrabold tracking-tight">뉴스레터 관리</h1>
        <NewsletterSendButton />
      </div>

      <p className="mb-6 text-sm text-[#6B6960]">
        &lsquo;주간 좌우 리포트&rsquo; 구독자 현황입니다. 매주 월요일 9시(KST)에 자동
        발송되며, 필요하면 위 버튼으로 즉시 발송할 수 있습니다.
      </p>

      {stats ? (
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-xl border border-line bg-white p-4 text-center">
            <p className="text-2xl font-extrabold text-ink">{stats.confirmed}</p>
            <p className="text-xs text-[#8A877E]">구독중</p>
          </div>
          <div className="rounded-xl border border-line bg-white p-4 text-center">
            <p className="text-2xl font-extrabold text-ink">{stats.pending}</p>
            <p className="text-xs text-[#8A877E]">확인 대기</p>
          </div>
        </div>
      ) : (
        <p className="text-sm text-[#8A877E]">통계를 불러오지 못했습니다.</p>
      )}
    </div>
  );
}
