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
      {/* 좌/우 구분은 색상으로 이미 충분해서(파랑/빨강) 카드마다 반복
          표시하지 않고 목록 전체에 한 번만 라벨을 둠 */}
      <div className="mb-1.5 grid grid-cols-2 px-1 text-[11px] font-bold">
        <span className="text-left-blue">좌</span>
        <span className="text-right text-right-red">우</span>
      </div>
      <PairCard pair={today} large />
      {rest.map((pair) => (
        <PairCard key={pair.id} pair={pair} />
      ))}
    </>
  );
}
