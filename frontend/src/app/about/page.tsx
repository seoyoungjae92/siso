import type { Metadata } from "next";
import Link from "next/link";

import { BackLink } from "@/components/BackLink";

const appName = "시소";
const CONTACT_EMAIL = "siso.contact.help@gmail.com";

export const metadata: Metadata = {
  title: `서비스 소개 - ${appName}`,
  description: "좌·우 커뮤니티 글을 나란히 모아보고, 같은 주제를 두고 익명으로 토론하는 공간",
};

export default function AboutPage() {
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <BackLink />
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">서비스 소개</h1>
      <p className="mb-8 text-sm text-[#767268]">같은 주제, 다른 시선</p>

      <div className="space-y-8 text-[14px] leading-relaxed text-[#33322E]">
        <section>
          <h2 className="mb-2 text-[16px] font-bold">{appName}는 무엇을 하는 곳인가요</h2>
          <p>
            {appName}는 좌 성향 커뮤니티(클리앙, 루리웹, 82쿡, 더쿠 등)와 우 성향 커뮤니티(디시인사이드
            정치 관련 갤러리 등)의 정치 관련 게시글을 자동으로 모아 나란히 보여주는 서비스입니다.
            같은 이슈를 다루는 좌·우 게시글을 찾아 AI가 양쪽 입장을 대칭적으로 요약해 &ldquo;같은
            주제, 다른 시선&rdquo;이라는 비교 토론 주제를 만듭니다. 회원가입 없이 누구나 익명으로
            투표하고 댓글을 남길 수 있습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">좌·우 분류 기준</h2>
          <p>
            좌·우 분류는 게시글이 수집된 커뮤니티 단위의 성향 기준이며, 개별 작성자 한 사람의 정치
            성향을 판단한 것이 아닙니다. 어느 커뮤니티를 어느 성향으로 분류할지는 운영자가 수동으로
            지정하며, 알고리즘이 자동으로 판단하지 않습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">놀이터 요약은 어떻게 만들어지나요</h2>
          <p>
            &ldquo;놀이터&rdquo;에 노출되는 주제 제목과 좌·우 입장 요약은 원문 게시글을 그대로
            옮긴 것이 아니라, AI가 여러 원문 게시글의 공통된 입장을 종합해 다시 작성한 것입니다.
            어조와 비속어 표현만 순화하며, 원문에 없는 사실을 새로 지어내지 않습니다. 분량과 어조는
            좌·우 어느 한쪽이 더 정당해 보이지 않도록 대칭을 맞춥니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">콘텐츠는 얼마나 자주 갱신되나요</h2>
          <p>
            수집·매칭·합성은 자동화된 배치로 주기적으로 실행되며, 생성된 지 일정 기간이 지난 주제는
            목록에서 자연히 제외됩니다. 원문 저작권은 각 원 커뮤니티 및 작성자에게 있으며, {appName}
            는 제목·요약·출처 링크만 수집해 병렬로 비교하는 서비스입니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">문의</h2>
          <p>
            서비스 이용 및 원문 삭제 요청 관련 문의는{" "}
            <Link href="/feedback" className="underline">
              제보·건의하기
            </Link>{" "}
            페이지 또는 {CONTACT_EMAIL}로 연락해주세요.
          </p>
        </section>
      </div>
    </div>
  );
}
