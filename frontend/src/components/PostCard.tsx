import { formatRelativeTime } from "@/lib/format";
import type { PostSummary, Side } from "@/lib/posts";

export const ACCENT: Record<Side, string> = {
  left: "text-left-blue",
  right: "text-right-red",
};

const BADGE: Record<Side, string> = {
  left: "bg-blue-tint text-left-blue",
  right: "bg-red-tint text-right-red",
};

export function PostCard({ post, side }: { post: PostSummary; side: Side }) {
  return (
    <article className="mb-2.5 rounded-[10px] border border-line bg-white p-3">
      <div className="mb-1.5 flex items-center justify-between gap-2">
        <span
          className={`inline-flex items-center gap-1 rounded-full px-1.5 py-0.5 text-[10.5px] font-bold ${BADGE[side]}`}
        >
          <span className="h-1 w-1 rounded-full bg-current" />
          {post.sourceName}
        </span>
        <time className="text-[11px] font-medium text-[#767268]">
          {formatRelativeTime(post.publishedAt ?? post.collectedAt)}
        </time>
      </div>
      <h3 className="mb-1 line-clamp-2 text-sm font-bold tracking-tight">{post.title}</h3>
      <div className="mt-2 text-[11px] text-[#767268]">
        <a href={post.originUrl} target="_blank" rel="noopener noreferrer">
          원문 보기 ↗
        </a>
      </div>
    </article>
  );
}
