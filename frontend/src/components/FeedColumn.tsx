import { AdSlot } from "@/components/AdSlot";
import { PostCard } from "@/components/PostCard";
import type { PostSummary, Side } from "@/lib/posts";

const COLUMN: Record<Side, { bg: string; heading: string; title: string }> = {
  left: { bg: "bg-blue-tint", heading: "text-left-blue", title: "좌 성향 커뮤니티" },
  right: { bg: "bg-red-tint", heading: "text-right-red", title: "우 성향 커뮤니티" },
};

const AD_AFTER_INDEX = 5;

export function FeedColumn({ side, posts }: { side: Side; posts: PostSummary[] }) {
  const { bg, heading, title } = COLUMN[side];

  return (
    <section className={`px-[18px] py-5 ${bg}`}>
      <div className="mb-3.5 flex items-baseline gap-2">
        <h2 className={`text-[15px] font-extrabold tracking-tight ${heading}`}>{title}</h2>
        <span className="text-xs text-[#8A877E]">실시간 수집</span>
      </div>

      {posts.map((post, index) => (
        <div key={post.id}>
          <PostCard post={post} side={side} />
          {index + 1 === AD_AFTER_INDEX && <AdSlot position={`feed-${side}`} />}
        </div>
      ))}
    </section>
  );
}
