import Link from "next/link";

import { AutoRefresh } from "@/components/AutoRefresh";
import { FeedColumn } from "@/components/FeedColumn";
import { MobileTabs } from "@/components/MobileTabs";
import { Playground } from "@/components/Playground";
import { fetchElectionMode } from "@/lib/election";
import { fetchFeaturedPair, fetchPairs } from "@/lib/pairs";
import { fetchTopPetitions } from "@/lib/petitions";
import { fetchPosts } from "@/lib/posts";

export default async function Home() {
  const [leftPosts, rightPosts, pairs, featured, petitions, electionMode] =
    await Promise.all([
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
      {/* 첫 방문자가 원문 목록만 보고 "스크랩 사이트"로 오해하지 않도록 서비스
          정체를 한 줄로 먼저 밝힌다 — 모바일은 헤더 태그라인도 숨겨져 있어
          설명이 전혀 없었음(2026-09-19 종합 검토). h1은 홈에 없던 것 보완. */}
      <section className="bg-white px-4 py-4 sm:px-7">
        <div className="mx-auto max-w-5xl">
          <h1 className="text-[20px] font-extrabold leading-tight tracking-tight sm:text-[24px]">
            같은 이슈, 좌·우는 이렇게 말합니다
          </h1>
          <p className="mt-1.5 text-[14px] leading-relaxed text-[#4A4842]">
            양쪽 커뮤니티에서 같은 사건을 찾아 AI가 같은 분량으로 정리하고,
            익명으로 토론합니다.{" "}
            <Link
              href="/about"
              className="font-semibold text-playground underline-offset-2 hover:underline"
            >
              자세히 보기
            </Link>
          </p>
        </div>
      </section>
      {/* 그리드 트랙 기본 최소 크기(auto)는 자식 콘텐츠의 min-content
          너비를 따라간다 — 우측 배지(디시인사이드 갤러리명 등)가 좌측보다
          길면 좁은 화면에서 1fr:1fr이어도 우측 트랙이 더 크게 밀림
          (완전한 대칭성 원칙 위반, 실측: 1024px에서 좌 302px/우 346px).
          minmax(0, ...)로 최소 크기를 0으로 눌러야 fr 비율이 항상 그대로
          지켜진다. */}
      <div className="hidden flex-1 grid-cols-[minmax(0,1fr)_minmax(0,1.35fr)_minmax(0,1fr)] lg:grid">
        <FeedColumn
          side="left"
          posts={leftPosts.posts}
          hasMore={leftPosts.hasMore}
        />
        <section className="min-h-full bg-pg-tint">
          <div className="px-[18px] pt-6 pb-3">
            <h2 className="text-[22px] font-extrabold tracking-tight text-playground">
              놀이터
            </h2>
            <p className="mt-1 text-[13px] text-[#6B6960]">
              양쪽 시각을 합성한 오늘의 토론 주제
            </p>
          </div>
          <div className="px-[18px] pb-5">
            <Playground
              pairs={pairs.pairs}
              hasMore={pairs.hasMore}
              featured={featured}
              petitions={petitions}
              hideVotes={electionMode}
            />
          </div>
        </section>
        <FeedColumn
          side="right"
          posts={rightPosts.posts}
          hasMore={rightPosts.hasMore}
        />
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
