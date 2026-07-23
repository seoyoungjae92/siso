import { fetchDashboard, requireAdmin } from "@/lib/admin";
import type { DashboardData } from "@/lib/admin";
import { formatRelativeTime } from "@/lib/format";

const STAT_LABELS: { key: Exclude<keyof DashboardData, "sourceStats">; label: string }[] = [
  { key: "totalComments", label: "총 댓글 수" },
  { key: "totalVotes", label: "총 투표 수" },
  { key: "pendingReports", label: "대기 중인 신고" },
  { key: "totalReports", label: "총 신고 수" },
  { key: "newAnonUsersLast24h", label: "최근 24시간 신규 익명 ID" },
  { key: "activeAnonUsersLast24h", label: "최근 24시간 댓글 작성 익명 ID" },
];

export default async function AdminDashboardPage() {
  await requireAdmin();
  const data = await fetchDashboard();

  if (!data) {
    return (
      <div className="mx-auto w-full max-w-3xl px-4 py-10">
        <p className="text-sm text-right-red">대시보드 데이터를 불러오지 못했습니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">대시보드</h1>

      <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {STAT_LABELS.map(({ key, label }) => (
          <div key={key} className="rounded-xl border border-line bg-white p-3">
            <div className="text-[11px] text-[#8A877E]">{label}</div>
            <div className="text-xl font-extrabold">{data[key]}</div>
          </div>
        ))}
      </div>

      <h2 className="mb-3 text-[15px] font-extrabold">소스별 수집 현황</h2>
      <div className="overflow-hidden rounded-xl border border-line bg-white">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-line bg-[#FBFAF7] text-xs text-[#8A877E]">
              <th className="px-3 py-2 font-semibold">이름</th>
              <th className="px-3 py-2 font-semibold">성향</th>
              <th className="px-3 py-2 font-semibold">게시글 수</th>
              <th className="px-3 py-2 font-semibold">최근 수집</th>
            </tr>
          </thead>
          <tbody>
            {data.sourceStats.map((stat) => (
              <tr key={stat.sourceName} className="border-b border-line last:border-b-0">
                <td className="px-3 py-2 font-bold">{stat.sourceName}</td>
                <td className={stat.side === "left" ? "px-3 py-2 text-left-blue" : "px-3 py-2 text-right-red"}>
                  {stat.side === "left" ? "좌" : "우"}
                </td>
                <td className="px-3 py-2">{stat.postCount}</td>
                <td className="px-3 py-2 text-[#8A877E]">
                  {stat.lastCollectedAt ? formatRelativeTime(stat.lastCollectedAt) : "수집된 글 없음"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
