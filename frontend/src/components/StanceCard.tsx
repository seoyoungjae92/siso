import { ACCENT } from "@/components/PostCard";
import type { Side } from "@/lib/posts";

// 좌/우 카드가 나란히 놓이니 테두리·라벨 위치도 바깥쪽으로 거울처럼
// 대칭시킨다(좌 카드는 왼쪽에 파란 테두리+라벨, 우 카드는 오른쪽에
// 빨간 테두리+라벨). 본문 문단은 가독성을 위해 항상 왼쪽 정렬 유지.
const CARD_STYLE: Record<Side, string> = {
  left: "border-l-4 border-l-left-blue bg-blue-tint text-left",
  right: "border-r-4 border-r-right-red bg-red-tint text-right",
};

const BULLET_STYLE: Record<Side, string> = {
  left: "marker:text-left-blue",
  right: "marker:text-right-red",
};

// points는 합성 단계에서 좌/우 개수를 같게 맞춰 저장된다(보강 없는 주제는
// 빈 배열) — 좌/우 카드가 항상 같은 구조로 렌더링된다.
export function StanceCard({
  side,
  text,
  points = [],
}: {
  side: Side;
  text: string;
  points?: string[];
}) {
  return (
    <article className={`rounded-[10px] p-4 ${CARD_STYLE[side]}`}>
      <p className={`mb-1.5 text-[12px] font-bold ${ACCENT[side]}`}>
        {side === "left" ? "좌 시각" : "우 시각"}
      </p>
      <p className="text-left text-[15px] leading-relaxed text-[#33322E]">
        {text}
      </p>
      {points.length > 0 && (
        <>
          <p className={`mb-1.5 mt-4 text-[12px] font-bold ${ACCENT[side]}`}>
            핵심 논거
          </p>
          <ul
            className={`list-disc space-y-1.5 pl-4 text-left text-[14px] leading-relaxed text-[#33322E] ${BULLET_STYLE[side]}`}
          >
            {points.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>
        </>
      )}
    </article>
  );
}
