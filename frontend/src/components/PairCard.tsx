import Link from "next/link";

import { ACCENT } from "@/components/PostCard";
import type { TopicPair } from "@/lib/pairs";
import type { Side } from "@/lib/posts";

function PairHalf({ side, title, summary }: { side: Side; title: string; summary: string }) {
  return (
    <div className="p-3">
      <h4 className={`mb-1 line-clamp-2 text-[13px] font-bold ${ACCENT[side]}`}>{title}</h4>
      <p className="line-clamp-2 text-[11.5px] text-[#6B6960]">{summary}</p>
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
      <div className="grid grid-cols-2 divide-x divide-line">
        <PairHalf side="left" title={pair.leftPost.title} summary={pair.leftPost.summary} />
        <PairHalf side="right" title={pair.rightPost.title} summary={pair.rightPost.summary} />
      </div>
      <div className="border-t border-line px-3.5 py-2 text-[11px] text-[#8A877E]">
        유사도 {Math.round(pair.similarity * 100)}%
      </div>
    </Link>
  );
}
