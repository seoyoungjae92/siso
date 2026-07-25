import { FeedColumn } from "@/components/FeedColumn";
import { MobileTabs } from "@/components/MobileTabs";
import { Playground } from "@/components/Playground";
import { fetchPairs } from "@/lib/pairs";
import { fetchTopPetitions } from "@/lib/petitions";
import { fetchPosts } from "@/lib/posts";

export default async function Home() {
  const [leftPosts, rightPosts, pairs, petitions] = await Promise.all([
    fetchPosts("left"),
    fetchPosts("right"),
    fetchPairs(),
    fetchTopPetitions(),
  ]);

  return (
    <div className="flex flex-1 flex-col">
      <div className="hidden flex-1 grid-cols-[1fr_1.35fr_1fr] lg:grid">
        <FeedColumn side="left" posts={leftPosts.posts} hasMore={leftPosts.hasMore} />
        <section className="bg-pg-tint px-[18px] py-5">
          <div className="mb-3.5">
            <h2 className="text-[15px] font-extrabold tracking-tight text-playground">놀이터</h2>
          </div>
          <Playground pairs={pairs.pairs} hasMore={pairs.hasMore} petitions={petitions} />
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
          petitions={petitions}
        />
      </div>
    </div>
  );
}
