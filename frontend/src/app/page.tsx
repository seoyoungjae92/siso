import { FeedColumn } from "@/components/FeedColumn";
import { Header } from "@/components/Header";
import { fetchPosts } from "@/lib/posts";

export default async function Home() {
  const [leftPosts, rightPosts] = await Promise.all([
    fetchPosts("left"),
    fetchPosts("right"),
  ]);

  return (
    <div className="flex flex-1 flex-col">
      <Header />
      <div className="grid flex-1 grid-cols-[1fr_1.35fr_1fr]">
        <FeedColumn side="left" posts={leftPosts} />
        <section className="bg-pg-tint px-[18px] py-5">
          <div className="mb-3.5 flex items-baseline gap-2">
            <h2 className="text-[15px] font-extrabold tracking-tight text-playground">
              플레이그라운드
            </h2>
            <span className="text-xs text-[#8A877E]">같은 주제, 좌우 병렬 비교</span>
          </div>
          <p className="text-sm text-[#6B6960]">준비 중입니다.</p>
        </section>
        <FeedColumn side="right" posts={rightPosts} />
      </div>
    </div>
  );
}
