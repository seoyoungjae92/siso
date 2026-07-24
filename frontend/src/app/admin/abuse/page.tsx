import { ResolveAlertButton } from "@/components/admin/ResolveAlertButton";
import { fetchAbuseAlerts, fetchIpClusters, requireAdmin } from "@/lib/admin";

const ALERT_TYPE_LABELS: Record<string, string> = {
  multi_account_same_ip: "동일 IP 다중 계정",
  activity_spike: "활동 급증",
};

function formatPayload(payload: Record<string, unknown>): string {
  return Object.entries(payload)
    .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.length + "건" : String(value)}`)
    .join(" · ");
}

export default async function AdminAbusePage() {
  await requireAdmin();
  const [unresolved, resolved, clusters] = await Promise.all([
    fetchAbuseAlerts(false),
    fetchAbuseAlerts(true),
    fetchIpClusters(),
  ]);

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">어뷰징 관리</h1>

      <h2 className="mb-4 text-lg font-extrabold tracking-tight">미해결 알림</h2>

      {unresolved.length === 0 && (
        <p className="text-sm text-[#6B6960]">미해결 알림이 없습니다.</p>
      )}

      <div className="space-y-4">
        {unresolved.map((alert) => (
          <div key={alert.id} className="rounded-xl border border-line bg-white p-4">
            <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
              <span className="font-semibold text-right-red">
                {ALERT_TYPE_LABELS[alert.type] ?? alert.type}
              </span>
              <span>{new Date(alert.createdAt).toLocaleString("ko-KR")}</span>
            </div>
            <p className="mb-3 text-[13px] text-[#6B6960]">{formatPayload(alert.payload)}</p>
            <ResolveAlertButton alertId={alert.id} />
          </div>
        ))}
      </div>

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">IP 클러스터 현황</h2>

      {clusters.length === 0 && (
        <p className="text-sm text-[#6B6960]">동일 IP에서 여러 익명 ID가 감지된 사례가 없습니다.</p>
      )}

      {clusters.length > 0 && (
        <div className="overflow-hidden rounded-xl border border-line bg-white">
          <table className="w-full text-left text-[13px]">
            <thead className="bg-[#FBFAF7] text-xs text-[#8A877E]">
              <tr>
                <th className="px-4 py-2 font-semibold">IP 해시</th>
                <th className="px-4 py-2 font-semibold">익명 ID 개수</th>
              </tr>
            </thead>
            <tbody>
              {clusters.map((cluster) => (
                <tr key={cluster.ipHash} className="border-t border-line">
                  <td className="px-4 py-2 font-mono text-xs">{cluster.ipHash.slice(0, 16)}…</td>
                  <td className="px-4 py-2">{cluster.anonUserCount}개</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">해결된 알림 이력</h2>

      {resolved.length === 0 && (
        <p className="text-sm text-[#6B6960]">해결된 알림이 없습니다.</p>
      )}

      <div className="space-y-3">
        {resolved.map((alert) => (
          <div key={alert.id} className="rounded-xl border border-line bg-white p-4">
            <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
              <span className="font-semibold">{ALERT_TYPE_LABELS[alert.type] ?? alert.type}</span>
              <span>{new Date(alert.createdAt).toLocaleString("ko-KR")}</span>
            </div>
            <p className="text-[13px] text-[#6B6960]">{formatPayload(alert.payload)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
