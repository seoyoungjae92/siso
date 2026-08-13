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

// "인기" 표시 기준 — 투표+댓글 합이 이 값 이상이면 뱃지를 붙인다. 현재
// 트래픽 기준으로 잡은 값이라(대부분 0~4), 트래픽이 늘면 같이 올려야 함.
const HOT_THRESHOLD = 5;

function CardMeta({ pair, hideVotes }: { pair: TopicPair; hideVotes: boolean }) {
  const showVoteCount = !hideVotes && pair.voteCount > 0;

  if (!showVoteCount && pair.commentCount === 0) {
    return null;
  }

  return (
    <div className="mt-2 flex items-center gap-3 text-[11px] text-[#767268]">
      {showVoteCount && (
        <span className="flex items-center gap-1">
          <VoteStampIcon />
          {pair.voteCount}명 투표
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
  // 오늘의 링(large)은 서버에서 이미 참여도 최상위로 골라 보내주는
  // 카드라 "인기" 표시가 항상 참이라 의미가 없다 — 목록 카드에서만 보여
  // 그중 눈에 띄는 것과 안 띄는 것을 구분해준다.
  const isHot = !large && !hideVotes && pair.voteCount + pair.commentCount >= HOT_THRESHOLD;

  return (
    <Link
      href={`/pairs/${pair.id}`}
      className={`mb-2.5 block overflow-hidden rounded-xl border border-line bg-white transition-shadow ${
        large
          ? "shadow-[0_8px_24px_rgba(110,61,116,.10)]"
          : "hover:shadow-[0_6px_20px_rgba(27,27,34,.08)]"
      }`}
    >
      {large && (
        <div className="flex items-center justify-between bg-gradient-to-r from-left-blue via-playground to-right-red px-4 py-2.5 text-white">
          <b className="flex items-center gap-1.5 text-[13px] tracking-wide">
            <BoxingGlovesIcon />
            오늘의 링
          </b>
        </div>
      )}
      <div className="p-4">
        <h4 className="mb-3 line-clamp-3 text-[15px] font-bold leading-snug text-ink">
          {pair.title}
          {isHot && (
            <span className="ml-1.5 inline-flex items-center rounded-full bg-pg-tint px-2 py-0.5 align-middle text-[10px] font-extrabold text-playground">
              🔥 인기
            </span>
          )}
        </h4>
        {!hideVotes && <MiniVoteBar pair={pair} />}
        <CardMeta pair={pair} hideVotes={hideVotes} />
      </div>
    </Link>
  );
}
