import { ImageResponse } from "next/og";

import { fetchPairDetail } from "@/lib/comments";
import { fetchElectionMode } from "@/lib/election";
import { calculateVotePercentages } from "@/lib/pairs";

export const alt = "시소 — 같은 주제, 다른 시선";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

// 사이트가 이미 쓰는 것과 동일한 폰트(Pretendard v1.3.9) — CSS @font-face로
// 로드하는 dynamic-subset 파일은 Satori에 못 넘기므로(임의 한글 글자를
// 커버 못 함), 한글 전체를 포함하는 정적(non-subset) TTF를 별도로 fetch한다.
const PRETENDARD_BOLD_URL =
  "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/packages/pretendard/dist/public/static/alternative/Pretendard-Bold.ttf";

function truncate(text: string, max: number): string {
  return text.length > max ? `${text.slice(0, max)}…` : text;
}

export default async function Image({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [pair, electionMode, fontData] = await Promise.all([
    fetchPairDetail(id),
    fetchElectionMode(),
    fetch(PRETENDARD_BOLD_URL).then((res) => res.arrayBuffer()),
  ]);

  const title = truncate(pair?.title ?? "시소 — 같은 주제, 다른 시선", 42);
  const { leftPct, neutralPct, rightPct, total } = pair
    ? calculateVotePercentages(pair)
    : { leftPct: 0, neutralPct: 0, rightPct: 0, total: 0 };
  const showVotes = !electionMode && total > 0;

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "#F7F6F3",
          padding: "64px 72px",
          fontFamily: "Pretendard",
        }}
      >
        <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
          <span style={{ fontSize: 40, fontWeight: 700, color: "#1B1B22" }}>시소</span>
          <span style={{ fontSize: 20, color: "#8A877E" }}>같은 주제, 다른 시선</span>
        </div>

        <div
          style={{
            display: "flex",
            fontSize: 56,
            fontWeight: 700,
            color: "#1B1B22",
            lineHeight: 1.3,
          }}
        >
          {title}
        </div>

        {/* 조건부 렌더링에 <>...</> Fragment를 쓰면 Satori가 레이아웃을
            잘못 계산해서(하위 트리 폭이 안 잡혀 글자 단위 줄바꿈이 남)
            실제 이미지가 깨진다 — 실측으로 확인. 반드시 실제 div로 감쌀 것. */}
        {showVotes ? (
          <div style={{ display: "flex", flexDirection: "column", width: "100%", gap: 12 }}>
            <div style={{ display: "flex", width: "100%", height: 28, borderRadius: 14, overflow: "hidden" }}>
              <div style={{ display: "flex", width: `${leftPct}%`, background: "#0047A0" }} />
              <div style={{ display: "flex", width: `${neutralPct}%`, background: "#6E3D74" }} />
              <div style={{ display: "flex", width: `${rightPct}%`, background: "#CD2E3A" }} />
            </div>
            <div
              style={{
                display: "flex",
                width: "100%",
                justifyContent: "center",
                fontSize: 22,
                color: "#6B6960",
              }}
            >
              {`좌에 공감 ${leftPct}%  ·  중립 ${neutralPct}%  ·  우에 공감 ${rightPct}%`}
            </div>
          </div>
        ) : (
          <div style={{ display: "flex", fontSize: 22, color: "#8A877E" }}>
            시소에서 이 주제에 대한 의견을 나눠보세요
          </div>
        )}
      </div>
    ),
    {
      ...size,
      fonts: [{ name: "Pretendard", data: fontData, style: "normal", weight: 700 }],
    },
  );
}
