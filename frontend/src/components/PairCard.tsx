import Link from "next/link";

import { VoteStampIcon } from "@/components/VoteStampIcon";
import { calculateVotePercentages, type TopicPair } from "@/lib/pairs";

// 상세 페이지(StanceCard)와 같은 거울 대칭 규칙 — 좌는 왼쪽 테두리+좌
// 정렬, 우는 오른쪽 테두리+우 정렬. 목록에서도 클릭 전에 실제 합성
// 내용을 보여주기 위한 2줄 미리보기(애드센스 "낮은 가치 콘텐츠" 반려
// 이후 추가 — 이전엔 제목만 보여 홈이 링크 목록처럼 얇게 보였음). "좌"/"우"
// 글자 라벨은 안 씀 — line-clamp-2로 잘리는 문단 끝에 라벨을 붙이면 긴
// 글에서 잘려 안 보임(우측에서 실제 발견된 버그), 테두리/정렬 색만으로도
// 구분되니 라벨 없이 감. 우측은 text-right라 line-clamp 말줄임(...)이
// 항상 컨테이너 오른쪽 끝에 붙어서 나오는데(좌측은 자연스러운 줄바꿈
// 위치라 안 그럼), pr-2로는 빨간 테두리 바와 겹쳐 보여 pr-3으로 여유를 둠.
function StancePreview({ pair }: { pair: TopicPair }) {
  return (
    <div className="mt-3 grid grid-cols-2 gap-2">
      <p className="line-clamp-2 border-l-2 border-l-left-blue pl-2.5 text-left text-[13.5px] leading-snug text-[#4A4842]">
        {pair.leftStance}
      </p>
      <p className="line-clamp-2 border-r-2 border-r-right-red pr-3 text-right text-[13.5px] leading-snug text-[#4A4842]">
        {pair.rightStance}
      </p>
    </div>
  );
}

function MiniVoteBar({ pair }: { pair: TopicPair }) {
  const { leftPct, neutralPct, rightPct, total } =
    calculateVotePercentages(pair);

  if (total === 0) {
    return null;
  }

  return (
    <div className="mt-3 flex h-1.5 w-full overflow-hidden rounded-full">
      <span
        className="block h-full bg-left-blue"
        style={{ width: `${leftPct}%` }}
      />
      <span
        className="block h-full bg-playground"
        style={{ width: `${neutralPct}%` }}
      />
      <span
        className="block h-full bg-right-red"
        style={{ width: `${rightPct}%` }}
      />
    </div>
  );
}

// "인기" 표시 기준 — 투표+댓글 합이 이 값 이상이면 뱃지를 붙인다. 현재
// 트래픽 기준으로 잡은 값이라(대부분 0~4), 트래픽이 늘면 같이 올려야 함.
const HOT_THRESHOLD = 5;

function CardMeta({
  pair,
  hideVotes,
}: {
  pair: TopicPair;
  hideVotes: boolean;
}) {
  const showVoteCount = !hideVotes && pair.voteCount > 0;

  if (!showVoteCount && pair.commentCount === 0) {
    return null;
  }

  return (
    <div className="mt-2.5 flex items-center gap-3 text-[13px] text-[#6B6960]">
      {showVoteCount && (
        <span className="flex items-center gap-1">
          <VoteStampIcon />
          {pair.voteCount}명 투표
        </span>
      )}
      {pair.commentCount > 0 && <span>댓글 {pair.commentCount}</span>}
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
  const isHot =
    !large && !hideVotes && pair.voteCount + pair.commentCount >= HOT_THRESHOLD;

  // 쉬고 있는 요소에 그림자·테두리를 두르는 대신, 바닥 틴트 위에 흰 면으로
  // 단계를 준다. "오늘의 링"은 그림자가 아니라 크기로 앞서게 한다.
  return (
    <Link
      href={`/pairs/${pair.id}`}
      className="mb-3 block overflow-hidden rounded-xl bg-white transition-colors hover:bg-[#FBFAF8]"
    >
      {large && (
        <div className="flex items-center justify-between bg-playground px-4 py-3 text-white">
          <b className="text-[14px] font-extrabold tracking-wide">오늘의 링</b>
        </div>
      )}
      <div className="p-4">
        <h4
          className={`mb-3 font-bold leading-snug tracking-tight text-ink ${
            large ? "line-clamp-3 text-[24px]" : "line-clamp-3 text-[16.5px]"
          }`}
        >
          {pair.title}
          {isHot && (
            <span className="ml-1.5 inline-flex items-center rounded-full bg-pg-tint px-2 py-0.5 align-middle text-[11px] font-extrabold text-playground">
              인기
            </span>
          )}
        </h4>
        <StancePreview pair={pair} />
        {!hideVotes && <MiniVoteBar pair={pair} />}
        <CardMeta pair={pair} hideVotes={hideVotes} />
      </div>
    </Link>
  );
}
