"use client";

import { useState, useTransition } from "react";

import { postReaction } from "@/app/pairs/[id]/actions";
import { CommentForm } from "@/components/CommentForm";
import type { Comment } from "@/lib/comments";
import { formatRelativeTime } from "@/lib/format";

const STANCE_LABEL: Record<string, string> = { left: "좌", right: "우", neutral: "중립" };
const STANCE_COLOR: Record<string, string> = {
  left: "bg-left-blue",
  right: "bg-right-red",
  neutral: "bg-playground",
};

function ReactionButton({
  pairId,
  commentId,
  type,
  count,
  active,
}: {
  pairId: string;
  commentId: number;
  type: "up" | "down";
  count: number;
  active: boolean;
}) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() =>
        startTransition(async () => {
          await postReaction(pairId, commentId, type);
        })
      }
      className={`rounded-full border px-2 py-0.5 text-[11px] font-bold disabled:opacity-50 ${
        active ? "border-playground bg-pg-tint text-playground" : "border-line text-[#8A877E]"
      }`}
    >
      {type === "up" ? "👍" : "👎"} {count}
    </button>
  );
}

function CommentRow({ pairId, comment }: { pairId: string; comment: Comment }) {
  return (
    <div className="rounded-[10px] border border-line bg-white p-2.5">
      <div className="mb-1 flex items-center gap-1.5 text-[11px] font-bold">
        <span>{comment.nickname}</span>
        {comment.stance && (
          <span
            className={`rounded-full px-1.5 py-0.5 text-[9.5px] font-extrabold text-white ${STANCE_COLOR[comment.stance]}`}
          >
            {STANCE_LABEL[comment.stance]}
          </span>
        )}
        {comment.selfReply && (
          <span className="rounded-full bg-[#8A877E] px-1.5 py-0.5 text-[9.5px] font-extrabold text-white">
            본인 댓글
          </span>
        )}
        <time className="ml-auto font-medium text-[#A09D94]">
          {formatRelativeTime(comment.createdAt)}
        </time>
      </div>
      <p className="mb-2 text-[13px]">{comment.body}</p>
      <div className="flex gap-1.5">
        <ReactionButton
          pairId={pairId}
          commentId={comment.id}
          type="up"
          count={comment.upCount}
          active={comment.myReaction === "up"}
        />
        <ReactionButton
          pairId={pairId}
          commentId={comment.id}
          type="down"
          count={comment.downCount}
          active={comment.myReaction === "down"}
        />
      </div>
    </div>
  );
}

export function CommentThread({ pairId, comments }: { pairId: string; comments: Comment[] }) {
  const [openReplyId, setOpenReplyId] = useState<number | null>(null);

  const topLevel = comments.filter((c) => c.parentId === null);
  const repliesByParent = new Map<number, Comment[]>();
  for (const comment of comments) {
    if (comment.parentId !== null) {
      const list = repliesByParent.get(comment.parentId) ?? [];
      list.push(comment);
      repliesByParent.set(comment.parentId, list);
    }
  }

  return (
    <div>
      <CommentForm pairId={pairId} />

      {topLevel.length === 0 && (
        <p className="text-sm text-[#6B6960]">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</p>
      )}

      <div className="flex flex-col gap-2.5">
        {topLevel.map((comment) => (
          <div key={comment.id}>
            <CommentRow pairId={pairId} comment={comment} />
            <button
              type="button"
              onClick={() => setOpenReplyId(openReplyId === comment.id ? null : comment.id)}
              className="mt-1 text-[11px] font-bold text-[#8A877E]"
            >
              답글
            </button>
            {openReplyId === comment.id && (
              <div className="mt-2">
                <CommentForm
                  pairId={pairId}
                  parentId={comment.id}
                  onSuccess={() => setOpenReplyId(null)}
                />
              </div>
            )}

            <div className="mt-2 flex flex-col gap-2 border-l-2 border-line pl-3">
              {(repliesByParent.get(comment.id) ?? []).map((reply) => (
                <CommentRow key={reply.id} pairId={pairId} comment={reply} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
