import { AutoRefresh } from "@/components/AutoRefresh";
import { FeedColumn } from "@/components/FeedColumn";
import { MobileTabs } from "@/components/MobileTabs";
import { Playground } from "@/components/Playground";
import { fetchElectionMode } from "@/lib/election";
import { fetchFeaturedPair, fetchPairs } from "@/lib/pairs";
import { fetchTopPetitions } from "@/lib/petitions";
import { fetchPosts } from "@/lib/posts";

export default async function Home() {
  const [leftPosts, rightPosts, pairs, featured, petitions, electionMode] = await Promise.all([
    fetchPosts("left"),
    fetchPosts("right"),
    fetchPairs(),
    fetchFeaturedPair(),
    fetchTopPetitions(),
    fetchElectionMode(),
  ]);

  return (
    <div className="flex flex-1 flex-col">
      <AutoRefresh />
      <div className="hidden flex-1 grid-cols-[1fr_1.35fr_1fr] lg:grid">
        <FeedColumn side="left" posts={leftPosts.posts} hasMore={leftPosts.hasMore} />
        <section className="min-h-full">
          <div className="border-b border-line bg-gradient-to-b from-pg-tint to-white px-[18px] py-5">
            <span className="mb-2 inline-block rounded-full bg-playground px-2.5 py-0.5 text-[10.5px] font-extrabold tracking-wide text-white">
              PLAYGROUND
            </span>
            <h2 className="text-[22px] font-extrabold tracking-tight text-playground">놀이터</h2>
            <p className="mt-0.5 text-xs text-[#767268]">양쪽 시각을 합성한 오늘의 토론 주제</p>
          </div>
          <div className="px-[18px] py-5">
            <Playground
              pairs={pairs.pairs}
              hasMore={pairs.hasMore}
              featured={featured}
              petitions={petitions}
              hideVotes={electionMode}
            />
          </div>
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
          featured={featured}
          petitions={petitions}
          hideVotes={electionMode}
        />
      </div>
    </div>
  );
}
