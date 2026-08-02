import { ACCENT } from "@/components/PostCard";
import type { Side } from "@/lib/posts";

// 좌/우 카드가 나란히 놓이니 테두리·라벨 위치도 바깥쪽으로 거울처럼
// 대칭시킨다(좌 카드는 왼쪽에 파란 테두리+라벨, 우 카드는 오른쪽에
// 빨간 테두리+라벨). 본문 문단은 가독성을 위해 항상 왼쪽 정렬 유지.
const CARD_STYLE: Record<Side, string> = {
  left: "border-l-4 border-l-left-blue text-left",
  right: "border-r-4 border-r-right-red text-right",
};

export function StanceCard({ side, text }: { side: Side; text: string }) {
  return (
    <article className={`rounded-[10px] border border-line bg-white p-3 ${CARD_STYLE[side]}`}>
      <p className={`mb-1 text-[11px] font-bold ${ACCENT[side]}`}>{side === "left" ? "좌 시각" : "우 시각"}</p>
      <p className="text-left text-[13px] text-[#6B6960]">{text}</p>
    </article>
  );
}
