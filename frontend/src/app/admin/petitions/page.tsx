import { PetitionSyncButton } from "@/components/admin/PetitionSyncButton";
import { fetchAdminPetitions, requireAdmin, type AdminPetition } from "@/lib/admin";

const OUTCOME_LABELS: Record<string, string> = {
  established: "성립",
  not_established: "미성립",
};

function PetitionRow({ petition }: { petition: AdminPetition }) {
  return (
    <div className="rounded-xl border border-line bg-white p-4">
      <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
        <span>
          접수 {petition.receivedAt}
          {petition.status === "closed" && petition.closedAt && (
            <> · 마감 {new Date(petition.closedAt).toLocaleString("ko-KR")}</>
          )}
        </span>
        {petition.status === "collecting" ? (
          <span className="rounded-full bg-pg-tint px-2 py-0.5 font-semibold text-playground">진행중</span>
        ) : (
          <span
            className={`rounded-full px-2 py-0.5 font-semibold ${
              petition.outcome === "established"
                ? "bg-left-blue/10 text-left-blue"
                : "bg-right-red/10 text-right-red"
            }`}
          >
            {OUTCOME_LABELS[petition.outcome ?? ""] ?? "마감"}
          </span>
        )}
      </div>
      <a
        href={petition.linkUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="mb-2 block text-[14px] font-bold text-ink underline decoration-line"
      >
        {petition.title}
      </a>
      <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-[#8A877E]">
        <span>동의 {petition.agreeCount.toLocaleString("ko-KR")}명</span>
        {petition.committeeName && <span>소관위 {petition.committeeName}</span>}
        {petition.achvRatio != null && <span>달성률 {petition.achvRatio}%</span>}
      </div>
    </div>
  );
}

export default async function AdminPetitionsPage() {
  await requireAdmin();
  const petitions = await fetchAdminPetitions();
  const collecting = petitions.filter((p) => p.status === "collecting");
  const closed = petitions.filter((p) => p.status === "closed");

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-extrabold tracking-tight">청원 관리</h1>
        <PetitionSyncButton />
      </div>

      <h2 className="mb-4 text-lg font-extrabold tracking-tight">진행중 ({collecting.length})</h2>
      {collecting.length === 0 ? (
        <p className="text-sm text-[#6B6960]">진행중인 청원이 없습니다.</p>
      ) : (
        <div className="space-y-3">
          {collecting.map((p) => (
            <PetitionRow key={p.pttId} petition={p} />
          ))}
        </div>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">마감 ({closed.length})</h2>
      {closed.length === 0 ? (
        <p className="text-sm text-[#6B6960]">마감된 청원이 없습니다.</p>
      ) : (
        <div className="space-y-3">
          {closed.map((p) => (
            <PetitionRow key={p.pttId} petition={p} />
          ))}
        </div>
      )}
    </div>
  );
}
