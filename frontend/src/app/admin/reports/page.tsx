import Link from "next/link";

import { ModerateButtons } from "@/components/admin/ModerateButtons";
import { fetchPendingReports, requireAdmin } from "@/lib/admin";

const REASON_LABELS: Record<string, string> = {
  abuse: "욕설·인신공격",
  hate: "혐오 표현",
  spam: "도배·스팸",
  etc: "기타",
};

export default async function AdminReportsPage() {
  await requireAdmin();
  const groups = await fetchPendingReports();

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">신고 관리</h1>

      {groups.length === 0 && (
        <p className="text-sm text-[#6B6960]">대기 중인 신고가 없습니다.</p>
      )}

      <div className="space-y-4">
        {groups.map((group) => (
          <div key={group.commentId} className="rounded-xl border border-line bg-white p-4">
            <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
              <span>{group.nickname}</span>
              {group.pairId != null && (
                <Link href={`/pairs/${group.pairId}`} className="font-semibold underline">
                  토론 보기 ↗
                </Link>
              )}
            </div>
            <p className="mb-3 text-[14px]">{group.commentBody}</p>
            <div className="mb-3 flex flex-wrap gap-x-3 gap-y-1 text-xs text-[#8A877E]">
              <span>총 {group.totalReports}건</span>
              {Object.entries(group.reasonCounts).map(([reason, count]) => (
                <span key={reason}>
                  {REASON_LABELS[reason] ?? reason} {count}건
                </span>
              ))}
              <span>최초 신고: {new Date(group.oldestReportAt).toLocaleString("ko-KR")}</span>
            </div>
            <ModerateButtons commentId={group.commentId} />
          </div>
        ))}
      </div>
    </div>
  );
}
