import type { Petition } from "@/lib/petitions";

export function PetitionWidget({ petitions }: { petitions: Petition[] }) {
  if (petitions.length === 0) {
    return null;
  }

  return (
    <div className="mb-2.5 overflow-hidden rounded-xl border border-line bg-white">
      <div className="flex items-center justify-between bg-playground px-4 py-2.5 text-white">
        <b className="text-[13px] tracking-wide">🔥 실시간 청원 랭킹</b>
      </div>
      <ol className="divide-y divide-line">
        {petitions.map((petition, index) => (
          <li key={petition.id}>
            <a
              href={petition.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2.5 px-4 py-2.5 hover:bg-pg-tint"
            >
              <span className="w-4 shrink-0 text-[13px] font-extrabold text-playground">
                {index + 1}
              </span>
              <span className="line-clamp-1 flex-1 text-[13px] font-medium text-ink">
                {petition.title}
              </span>
              <span className="shrink-0 text-[11px] text-[#8A877E]">
                {petition.agreeCount.toLocaleString("ko-KR")}명
              </span>
            </a>
          </li>
        ))}
      </ol>
    </div>
  );
}
