import { formatRelativeTime } from "@/lib/format";
import { maskProfanity } from "@/lib/profanity";
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
    <article className="mb-2.5 rounded-[10px] border border-line bg-white p-3 transition-shadow hover:shadow-[0_4px_16px_rgba(27,27,34,.08)]">
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
      {/* 원문 발췌(summary)는 표시하지 않는다 — 크롤러가 상세 본문 앞부분을
          그대로 잘라 담는 경우가 많아 비속어 노출(종합 검토: 이틀간 29건)과
          원문 복제 리스크(CLAUDE.md 19.3, "발췌보다 재작성")가 함께 있었음.
          제목 + 출처 + 원문 링크만으로 목록 역할은 충분하다. */}
      <h3 className="mb-1 line-clamp-2 text-sm font-bold tracking-tight">{maskProfanity(post.title)}</h3>
      <div className="mt-2 text-[11px] text-[#767268]">
        <a href={post.originUrl} target="_blank" rel="noopener noreferrer">
          원문 보기 ↗
        </a>
      </div>
    </article>
  );
}
