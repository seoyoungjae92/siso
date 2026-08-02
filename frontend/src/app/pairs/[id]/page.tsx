import Link from "next/link";
import { notFound } from "next/navigation";

import { AdSlot } from "@/components/AdSlot";
import { CommentThread } from "@/components/CommentThread";
import { ShareButton } from "@/components/ShareButton";
import { StanceCard } from "@/components/StanceCard";
import { VoteWidget } from "@/components/VoteWidget";
import { fetchComments, fetchPairDetail } from "@/lib/comments";
import { fetchElectionMode } from "@/lib/election";

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
        <div className="mb-4 grid grid-cols-2 gap-3">
          <StanceCard side="left" text={pair.leftStance} />
          <StanceCard side="right" text={pair.rightStance} />
        </div>
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
