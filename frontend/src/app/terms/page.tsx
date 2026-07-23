import type { Metadata } from "next";

const appName = process.env.APP_NAME ?? "시소";
const CONTACT_EMAIL = "[문의 이메일을 입력하세요]";
const EFFECTIVE_DATE = "[시행일을 입력하세요 (예: 2026-08-01)]";

export const metadata: Metadata = {
  title: `이용약관 - ${appName}`,
};

export default function TermsPage() {
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">이용약관</h1>
      <p className="mb-8 text-sm text-[#8A877E]">시행일: {EFFECTIVE_DATE}</p>

      <div className="space-y-8 text-[14px] leading-relaxed text-[#33322E]">
        <section>
          <h2 className="mb-2 text-[16px] font-bold">제1조 (목적)</h2>
          <p>
            이 약관은 {appName}(이하 &ldquo;서비스&rdquo;)가 제공하는 서비스의 이용조건 및 절차,
            이용자와 서비스 운영자의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제2조 (서비스의 내용)</h2>
          <p>서비스는 다음과 같은 기능을 제공합니다.</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>좌·우 성향 커뮤니티에 게시된 글의 제목, 요약, 출처를 병렬로 모아 보여주는 피드</li>
            <li>같은 주제로 매칭된 좌·우 글을 비교해 볼 수 있는 &ldquo;플레이그라운드&rdquo;</li>
            <li>회원가입 없이 익명으로 참여할 수 있는 댓글, 추천, 입장 투표</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제3조 (계정 없는 익명 서비스)</h2>
          <p>
            서비스는 별도의 회원가입 절차 없이 누구나 이용할 수 있습니다. 이용자를 구분하기 위해
            브라우저 쿠키 기반의 익명 식별자를 사용하며, 자세한 내용은 개인정보처리방침을
            따릅니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제4조 (이용자의 의무)</h2>
          <p>이용자는 서비스 이용 시 다음 행위를 해서는 안 됩니다.</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>법령을 위반하거나 타인의 권리(명예, 초상권, 저작권 등)를 침해하는 게시물 작성</li>
            <li>욕설, 비속어, 인신공격, 혐오 표현이 포함된 게시물 작성</li>
            <li>동일하거나 유사한 내용을 반복 게시하는 도배·스팸 행위</li>
            <li>다수의 익명 식별자를 생성해 투표·추천·댓글 수를 부정하게 조작하는 행위</li>
            <li>서비스의 정상적인 운영을 방해하는 행위(자동화된 요청 남용 등)</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제5조 (게시물의 관리)</h2>
          <p>
            서비스는 게시물을 상시 사전 모니터링할 의무를 지지 않으며, 신고 등을 통해 위반 사실을
            구체적으로 인식한 경우 관리자 검토를 거쳐 해당 게시물을 삭제하거나 노출을 제한
            (블라인드)할 수 있습니다. 이용자는 자신이 작성한 게시물에 대한 책임을 지며, 게시물로
            인해 발생하는 분쟁에 대해 서비스는 원칙적으로 책임을 지지 않습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제6조 (저작권)</h2>
          <p>
            서비스가 수집하는 각 커뮤니티의 글은 제목, 200자 이내 요약, 원문 링크로만
            구성되며, 원문 전체나 이미지를 복제하지 않습니다. 원문에 대한 저작권은 원 작성자 및
            해당 커뮤니티에 있으며, 원문 확인은 제공된 링크를 통해 원 사이트에서 이루어집니다.
            원 게시물의 삭제를 원하는 작성자 또는 커뮤니티 운영자는 개인정보처리방침에 안내된
            연락처로 요청할 수 있습니다. 이용자가 서비스 내에 직접 작성한 댓글 등 게시물의
            저작권은 해당 이용자에게 있으며, 서비스는 게시물의 노출·운영에 필요한 범위 내에서
            이를 이용할 수 있습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제7조 (광고)</h2>
          <p>
            서비스는 운영 비용 충당을 위해 광고를 게재할 수 있습니다. 광고는 지정된 위치에만
            노출되며, 위반 사유로 노출이 제한(블라인드)된 게시물 주변에는 광고를 게재하지
            않습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제8조 (면책조항)</h2>
          <p>
            서비스는 천재지변, 시스템 점검, 원 커뮤니티의 사정 등 통제할 수 없는 사유로 인한
            서비스 중단에 대해 책임을 지지 않습니다. 서비스가 수집·노출하는 요약 및 링크는 원
            게시물 작성자의 견해를 요약한 것으로, 서비스 운영자의 입장을 대변하지 않으며 그
            정확성·완전성을 보장하지 않습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제9조 (약관의 변경)</h2>
          <p>
            서비스는 필요한 경우 이 약관을 변경할 수 있으며, 변경 시 서비스 내 공지를 통해
            알립니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">제10조 (문의)</h2>
          <p>약관 및 서비스 이용 관련 문의: {CONTACT_EMAIL}</p>
        </section>
      </div>
    </div>
  );
}
