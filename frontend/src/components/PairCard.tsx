import Link from "next/link";

import { calculateVotePercentages, type TopicPair } from "@/lib/pairs";

function MiniVoteBar({ pair }: { pair: TopicPair }) {
  const { leftPct, neutralPct, rightPct, total } = calculateVotePercentages(pair);

  if (total === 0) {
    return <div className="h-1.5 w-full rounded-full bg-line" />;
  }

  return (
    <div className="flex h-1.5 w-full overflow-hidden rounded-full">
      <span className="block h-full bg-left-blue" style={{ width: `${leftPct}%` }} />
      <span className="block h-full bg-playground" style={{ width: `${neutralPct}%` }} />
      <span className="block h-full bg-right-red" style={{ width: `${rightPct}%` }} />
    </div>
  );
}

export function PairCard({ pair, large = false }: { pair: TopicPair; large?: boolean }) {
  return (
    <Link
      href={`/pairs/${pair.id}`}
      className={`mb-2.5 block overflow-hidden rounded-xl border border-line bg-white ${large ? "shadow-[0_8px_24px_rgba(110,61,116,.10)]" : ""}`}
    >
      {large && (
        <div className="flex items-center justify-between bg-playground px-4 py-2.5 text-white">
          <b className="text-[13px] tracking-wide">🔥 오늘의 링</b>
        </div>
      )}
      <div className="p-4">
        <h4 className="mb-3 line-clamp-3 text-[15px] font-bold leading-snug text-ink">{pair.title}</h4>
        <MiniVoteBar pair={pair} />
      </div>
    </Link>
  );
}
