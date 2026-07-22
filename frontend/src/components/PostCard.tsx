import { formatRelativeTime } from "@/lib/format";
import type { PostSummary, Side } from "@/lib/posts";

const ACCENT: Record<Side, string> = {
  left: "text-left-blue",
  right: "text-right-red",
};

export function PostCard({ post, side }: { post: PostSummary; side: Side }) {
  return (
    <article className="mb-2.5 rounded-[10px] border border-line bg-white p-3">
      <div className={`mb-1 flex justify-between text-[11px] font-bold ${ACCENT[side]}`}>
        <span>{post.sourceName}</span>
        <time className="font-medium text-[#A09D94]">
          {formatRelativeTime(post.collectedAt)}
        </time>
      </div>
      <h3 className="mb-1 line-clamp-2 text-sm font-bold tracking-tight">{post.title}</h3>
      <p className="line-clamp-2 text-[12.5px] text-[#6B6960]">{post.summary}</p>
      <div className="mt-2 text-[11px] text-[#A09D94]">
        <a href={post.originUrl} target="_blank" rel="noopener noreferrer">
          원문 보기 ↗
        </a>
      </div>
    </article>
  );
}
