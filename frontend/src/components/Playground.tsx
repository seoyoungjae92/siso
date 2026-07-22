import { PairCard } from "@/components/PairCard";
import type { TopicPair } from "@/lib/pairs";

export function Playground({ pairs }: { pairs: TopicPair[] }) {
  if (pairs.length === 0) {
    return <p className="text-sm text-[#6B6960]">아직 매칭된 주제가 없습니다.</p>;
  }

  // "오늘의 링" 선정 기준: 현재는 최근 매칭순(첫 번째 pair) — 댓글/투표
  // 데이터가 없어 진짜 "인기"를 계산할 수 없는 임시 기준. M3에서 댓글/투표가
  // 생기면 그걸로 교체할 것.
  const [today, ...rest] = pairs;

  return (
    <>
      <PairCard pair={today} large />
      {rest.map((pair) => (
        <PairCard key={pair.id} pair={pair} />
      ))}
    </>
  );
}
