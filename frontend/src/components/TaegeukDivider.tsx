// PC 3단 레이아웃 컬럼 사이의 태극 S-커브 구분선 — 디자인 토큰(CLAUDE.md
// 11절)엔 "시그니처 요소"로 적혀 있었지만 실제 구현엔 빠져있던 것을
// 추가함(2026-08-13 디자인 리뷰). 컬럼 높이가 콘텐츠에 따라 계속
// 늘어나므로(무한스크롤), 인라인 <svg> 하나를 세로로 늘리면 커브가
// 거의 안 보이게 눌린다 — 대신 한 파장(120px)짜리 타일을
// background-repeat로 반복해서 높이와 무관하게 같은 리듬을 유지한다.
function tile(color: string) {
  const d = "M13,0 C13,30 26,30 26,60 C26,90 13,90 13,120";
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="26" height="120"><path d="${d}" fill="none" stroke="${color}" stroke-width="1.4" opacity=".3"/></svg>`;
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

export function TaegeukDivider({ side }: { side: "left" | "right" }) {
  const color = side === "left" ? "#0047A0" : "#CD2E3A";

  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute top-0 bottom-0 z-10 hidden w-[26px] lg:block"
      style={{
        [side]: "calc(100%/3.35 - 13px)",
        backgroundImage: tile(color),
        backgroundRepeat: "repeat-y",
      }}
    />
  );
}
