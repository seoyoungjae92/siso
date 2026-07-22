import { notFound } from "next/navigation";

import { CommentThread } from "@/components/CommentThread";
import { Header } from "@/components/Header";
import { PostCard } from "@/components/PostCard";
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
      <Header />
      <div className="mx-auto w-full max-w-3xl px-4 py-6">
        <div className="mb-4 grid grid-cols-2 gap-3">
          <PostCard post={pair.leftPost} side="left" />
          <PostCard post={pair.rightPost} side="right" />
        </div>
        <div className="mb-4 text-xs text-[#8A877E]">
          유사도 {Math.round(pair.similarity * 100)}%
        </div>
        <CommentThread pairId={id} comments={comments} />
      </div>
    </div>
  );
}
