import { FeedColumn } from "@/components/FeedColumn";
import { Header } from "@/components/Header";
import { MobileTabs } from "@/components/MobileTabs";
import { Playground } from "@/components/Playground";
import { fetchPairs } from "@/lib/pairs";
import { fetchPosts } from "@/lib/posts";

export default async function Home() {
  const [leftPosts, rightPosts, pairs] = await Promise.all([
    fetchPosts("left"),
    fetchPosts("right"),
    fetchPairs(),
  ]);

  return (
    <div className="flex flex-1 flex-col">
      <Header />
      <div className="hidden flex-1 grid-cols-[1fr_1.35fr_1fr] lg:grid">
        <FeedColumn side="left" posts={leftPosts.posts} hasMore={leftPosts.hasMore} />
        <section className="bg-pg-tint px-[18px] py-5">
          <div className="mb-3.5 flex items-baseline gap-2">
            <h2 className="text-[15px] font-extrabold tracking-tight text-playground">
              플레이그라운드
            </h2>
            <span className="text-xs text-[#8A877E]">같은 주제, 좌우 병렬 비교</span>
          </div>
          <Playground pairs={pairs.pairs} hasMore={pairs.hasMore} />
        </section>
        <FeedColumn side="right" posts={rightPosts.posts} hasMore={rightPosts.hasMore} />
      </div>
      <div className="flex flex-1 flex-col lg:hidden">
        <MobileTabs
          leftPosts={leftPosts.posts}
          leftHasMore={leftPosts.hasMore}
          rightPosts={rightPosts.posts}
          rightHasMore={rightPosts.hasMore}
          pairs={pairs.pairs}
          pairsHasMore={pairs.hasMore}
        />
      </div>
    </div>
  );
}
