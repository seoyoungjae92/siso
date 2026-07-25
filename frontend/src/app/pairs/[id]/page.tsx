import Link from "next/link";
import { notFound } from "next/navigation";

import { AdSlot } from "@/components/AdSlot";
import { CommentThread } from "@/components/CommentThread";
import { StanceCard } from "@/components/StanceCard";
import { VoteWidget } from "@/components/VoteWidget";
import { fetchComments, fetchPairDetail } from "@/lib/comments";

export default async function PairDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const [pair, comments] = await Promise.all([fetchPairDetail(id), fetchComments(id)]);

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
        <p className="mb-4 text-[12px] text-[#8A877E]">
          🤖 AI가 좌·우 커뮤니티 원문을 분석해 합성한 주제입니다. 원문의 비속어·저품질 표현은
          순화되며, 원문에 없는 사실은 추가하지 않습니다.
        </p>
        <div className="mb-4 grid grid-cols-2 gap-3">
          <StanceCard side="left" text={pair.leftStance} />
          <StanceCard side="right" text={pair.rightStance} />
        </div>
        <VoteWidget pairId={id} pair={pair} />
        <div className="mt-4">
          <AdSlot position="discussion" />
        </div>
        <CommentThread pairId={id} comments={comments} />
      </div>
    </div>
  );
}
