import Link from "next/link";

import { ACCENT } from "@/components/PostCard";
import type { TopicPair } from "@/lib/pairs";
import type { Side } from "@/lib/posts";

function StanceHalf({ side, text }: { side: Side; text: string }) {
  return (
    <div className="p-3">
      <p className={`mb-1 text-[11px] font-bold ${ACCENT[side]}`}>{side === "left" ? "좌 시각" : "우 시각"}</p>
      <p className="line-clamp-3 text-[12.5px] text-[#6B6960]">{text}</p>
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
      <h4 className="line-clamp-2 px-3.5 pt-3 text-[13px] font-bold text-ink">{pair.title}</h4>
      <div className="grid grid-cols-2 divide-x divide-line">
        <StanceHalf side="left" text={pair.leftStance} />
        <StanceHalf side="right" text={pair.rightStance} />
      </div>
    </Link>
  );
}
