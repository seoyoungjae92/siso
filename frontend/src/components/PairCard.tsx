import Link from "next/link";

import { BoxingGlovesIcon } from "@/components/BoxingGlovesIcon";
import { VoteStampIcon } from "@/components/VoteStampIcon";
import { calculateVotePercentages, type TopicPair } from "@/lib/pairs";

function MiniVoteBar({ pair }: { pair: TopicPair }) {
  const { leftPct, neutralPct, rightPct, total } = calculateVotePercentages(pair);

  if (total === 0) {
    return null;
  }

  return (
    <div className="flex h-1.5 w-full overflow-hidden rounded-full">
      <span className="block h-full bg-left-blue" style={{ width: `${leftPct}%` }} />
      <span className="block h-full bg-playground" style={{ width: `${neutralPct}%` }} />
      <span className="block h-full bg-right-red" style={{ width: `${rightPct}%` }} />
    </div>
  );
}

function CardMeta({ pair, hideVotes }: { pair: TopicPair; hideVotes: boolean }) {
  const { total } = calculateVotePercentages(pair);
  const voteCount = Math.round(total);
  const showVoteCount = !hideVotes && voteCount > 0;

  if (!showVoteCount && pair.commentCount === 0) {
    return null;
  }

  return (
    <div className="mt-2 flex items-center gap-3 text-[11px] text-[#767268]">
      {showVoteCount && (
        <span className="flex items-center gap-1">
          <VoteStampIcon />
          {voteCount}명 투표
        </span>
      )}
      {pair.commentCount > 0 && <span>💬 {pair.commentCount}</span>}
    </div>
  );
}

export function PairCard({
  pair,
  large = false,
  hideVotes = false,
}: {
  pair: TopicPair;
  large?: boolean;
  hideVotes?: boolean;
}) {
  return (
    <Link
      href={`/pairs/${pair.id}`}
      className={`mb-2.5 block overflow-hidden rounded-xl border border-line bg-white ${large ? "shadow-[0_8px_24px_rgba(110,61,116,.10)]" : ""}`}
    >
      {large && (
        <div className="flex items-center justify-between bg-playground px-4 py-2.5 text-white">
          <b className="flex items-center gap-1.5 text-[13px] tracking-wide">
            <BoxingGlovesIcon />
            오늘의 링
          </b>
        </div>
      )}
      <div className="p-4">
        <h4 className="mb-3 line-clamp-3 text-[15px] font-bold leading-snug text-ink">{pair.title}</h4>
        {!hideVotes && <MiniVoteBar pair={pair} />}
        <CardMeta pair={pair} hideVotes={hideVotes} />
      </div>
    </Link>
  );
}
