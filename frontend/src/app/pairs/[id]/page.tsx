import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { AdSlot } from "@/components/AdSlot";
import { CommentThread } from "@/components/CommentThread";
import { ShareButton } from "@/components/ShareButton";
import { StanceCard } from "@/components/StanceCard";
import { VoteWidget } from "@/components/VoteWidget";
import { fetchComments, fetchPairDetail } from "@/lib/comments";
import { fetchElectionMode } from "@/lib/election";
import { isEnriched } from "@/lib/pairs";

// 이게 없으면 모든 주제 상세 페이지가 루트 layout의 사이트 전역
// title/description을 그대로 물려받아서, 검색엔진·SNS 공유·AI
// 크롤러 모두 어느 주제 페이지든 똑같은 제목("시소 — 같은 주제,
// 다른 시선")으로만 보게 됨 — og:image는 페이지별로 이미 자동
// 생성되는데(opengraph-image.tsx) title/description만 빠져있었음.
export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const pair = await fetchPairDetail(id);

  if (!pair) {
    return { title: "존재하지 않는 주제" };
  }

  // 보강된 주제는 중립적인 쟁점 배경이 좌/우 한쪽을 먼저 보여주는 것보다
  // 설명으로 더 적합하다(대칭성) — 보강 없는 주제만 기존 방식 유지.
  const raw = pair.background ?? `좌: ${pair.leftStance} 우: ${pair.rightStance}`;
  const description = raw.length > 150 ? `${raw.slice(0, 150)}…` : raw;

  return {
    title: pair.title,
    description,
    openGraph: { title: pair.title, description },
    twitter: { title: pair.title, description },
    // 보강 없는 주제는 본문이 좌우 요약 몇 줄뿐이라 검색 색인에서 제외 —
    // 애드센스 "가치가 별로 없는 콘텐츠" 반려(2026-09) 대응. 링크는 계속
    // 따라가도록 follow는 유지.
    ...(isEnriched(pair) ? {} : { robots: { index: false, follow: true } }),
  };
}

export default async function PairDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const [pair, comments, electionMode] = await Promise.all([
    fetchPairDetail(id),
    fetchComments(id),
    fetchElectionMode(),
  ]);

  if (!pair) {
    notFound();
  }

  // 배포 순서상 프론트가 백엔드보다 먼저 올라가면 보강 필드가 아예 없는
  // 응답을 받을 수 있어 방어적으로 기본값을 둔다.
  const discussionQuestions = pair.discussionQuestions ?? [];

  return (
    <div className="flex flex-1 flex-col">
      <div className="mx-auto w-full max-w-3xl px-4 py-6">
        <Link
          href="/"
          className="mb-4 inline-block text-[13px] font-bold text-[#6B6960]"
        >
          ← 목록으로
        </Link>
        <h1 className="mb-2 text-lg font-extrabold tracking-tight">{pair.title}</h1>
        <ShareButton title={pair.title} />
        <p className="mb-4 text-[12px] text-[#767268]">
          🤖 AI가 좌·우 커뮤니티 원문을 분석해 합성한 주제입니다. 원문의 비속어·저품질 표현은
          순화되며, 원문에 없는 사실은 추가하지 않습니다.
        </p>
        {/* gap이 배지(h-9=36px)보다 넓어야(gap-10=40px) 배지가 카드 안쪽
            padding/텍스트 영역을 절대 침범하지 않는다 — 좌우 글 길이가
            달라 배지가 세로 어느 위치에 오든 안전(텍스트 가림 버그 수정,
            2026-08-13). */}
        {pair.background && (
          <section className="mb-4 rounded-[10px] border border-line bg-white p-3">
            <h2 className="mb-1 text-[11px] font-bold text-playground">쟁점 배경</h2>
            <p className="text-[13px] leading-relaxed text-[#4A4842]">{pair.background}</p>
          </section>
        )}
        <div className="relative mb-4 grid grid-cols-2 gap-10">
          <StanceCard side="left" text={pair.leftStance} points={pair.leftPoints} />
          <StanceCard side="right" text={pair.rightStance} points={pair.rightPoints} />
          <span
            aria-hidden="true"
            className="absolute left-1/2 top-1/2 z-10 flex h-9 w-9 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-playground text-[10px] font-black tracking-wide text-white shadow-[0_6px_16px_rgba(110,61,116,.35),0_0_0_4px_var(--paper)]"
          >
            VS
          </span>
        </div>
        {discussionQuestions.length > 0 && (
          <section className="mb-4 rounded-[10px] border border-line bg-white p-3">
            <h2 className="mb-1 text-[11px] font-bold text-playground">생각해볼 질문</h2>
            <ol className="list-decimal space-y-1 pl-4 text-[13px] text-[#4A4842] marker:font-bold marker:text-playground">
              {discussionQuestions.map((question) => (
                <li key={question}>{question}</li>
              ))}
            </ol>
          </section>
        )}
        {electionMode ? (
          <p className="mb-4 rounded-[10px] border border-line bg-[#F5F4F0] px-3 py-2.5 text-[12px] text-[#6B6960]">
            선거 기간 중에는 투표 기능이 일시 중단됩니다.
          </p>
        ) : (
          <VoteWidget pairId={id} pair={pair} />
        )}
        <div className="mt-4">
          <AdSlot position="discussion" />
        </div>
        <CommentThread pairId={id} comments={comments} />
      </div>
    </div>
  );
}
