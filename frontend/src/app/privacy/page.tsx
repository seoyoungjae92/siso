import type { Metadata } from "next";

const appName = process.env.APP_NAME ?? "시소";
const CONTACT_EMAIL = "[문의 이메일을 입력하세요]";
const EFFECTIVE_DATE = "[시행일을 입력하세요 (예: 2026-08-01)]";

export const metadata: Metadata = {
  title: `개인정보처리방침 - ${appName}`,
};

export default function PrivacyPage() {
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">개인정보처리방침</h1>
      <p className="mb-8 text-sm text-[#8A877E]">시행일: {EFFECTIVE_DATE}</p>

      <div className="space-y-8 text-[14px] leading-relaxed text-[#33322E]">
        <section>
          <p>
            {appName}(이하 &ldquo;서비스&rdquo;)는 회원가입 없이 누구나 읽고 쓸 수 있는 익명
            기반 서비스입니다. 이 방침은 서비스 운영 과정에서 어떤 정보를 수집하고, 어떤 목적으로
            사용하며, 얼마나 보관하는지를 설명합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">1. 수집하는 개인정보 항목</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              <b>익명 식별자(쿠키)</b>: 최초 방문 시 발급되는 무작위 값(UUID)입니다. 이름, 이메일
              등 신원을 알 수 있는 정보는 포함되지 않으며 회원 가입 절차 자체가 없습니다.
            </li>
            <li>
              <b>IP 주소 해시</b>: 원본 IP 주소는 저장하지 않고, 복원할 수 없는 형태(salt +
              SHA-256 해시)로만 저장합니다.
            </li>
            <li>
              <b>접속 로그</b>: 서비스 이용 기록(방문 일시, 요청 경로 등 일반적인 서버 접속
              로그)입니다.
            </li>
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">2. 수집 목적</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li>동일 이용자의 중복 투표·댓글 방지, 여론 조작(어뷰징) 탐지 및 방지</li>
            <li>댓글·투표 등 부정 이용 방지를 위한 요청 빈도 제한(레이트 리밋)</li>
            <li>신고 처리 및 부정 이용 조사</li>
            <li>서비스 안정적 운영, 장애 대응, 통계 분석</li>
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">3. 보유 및 이용 기간</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li>IP 주소 해시: 수집일로부터 90일 후 자동 파기</li>
            <li>접속 로그: 수집일로부터 90일 후 파기</li>
            <li>
              익명 식별자(쿠키) 및 이에 연계된 활동 통계(댓글 수, 투표 수 등)는 서비스 제공을 위해
              보관하며, 브라우저에서 쿠키를 삭제하면 이후에는 새 익명 식별자가 발급됩니다.
            </li>
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">4. 개인정보의 제3자 제공</h2>
          <p>
            이용자의 개인정보는 원칙적으로 외부에 제공하지 않습니다. 다만 법령에 근거가 있거나
            수사기관이 법령에서 정한 절차와 방법에 따라 요청하는 경우는 예외로 합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">5. 개인정보 처리 위탁</h2>
          <p>
            현재 개인정보 처리를 외부에 위탁하고 있지 않습니다. 향후 신고 처리 자동화 등을 위해
            외부 사업자(예: AI 분석 서비스)에 위탁하게 될 경우, 위탁 내용과 수탁자를 사전에 이
            방침에 반영하여 고지합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">6. 쿠키 운영 및 거부 방법</h2>
          <p>
            서비스는 익명 식별을 위해 쿠키를 사용합니다. 브라우저 설정에서 쿠키 저장을 거부할 수
            있으나, 이 경우 투표 중복 방지, 댓글 작성 등 일부 기능이 정상적으로 동작하지 않을 수
            있습니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">7. 수집된 게시물의 삭제 요청</h2>
          <p>
            서비스는 각 커뮤니티에 게시된 글의 제목, 200자 이내 요약, 원문 링크만 수집·노출하며
            본문 전체나 이미지를 복제하지 않습니다. 원 게시물의 작성자 또는 운영자가 삭제를
            요청하는 경우 아래 연락처로 접수해 주시면 영업일 기준 24시간 이내 1차 검토 후
            처리합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">8. 이용자의 권리</h2>
          <p>
            이용자는 자신과 관련된 정보의 처리 현황에 대해 문의하거나 삭제를 요청할 수 있습니다.
            다만 서비스는 회원 식별 정보를 보관하지 않으므로, 특정 익명 식별자(쿠키 값)를 함께
            알려주셔야 조회가 가능합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">9. 개인정보의 안전성 확보 조치</h2>
          <p>
            IP 주소는 원본을 저장하지 않고 해시로만 보관하며, 접근 권한을 최소화하는 등 개인정보가
            분실·도난·유출·변조되지 않도록 관리합니다.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">10. 개인정보 보호책임자</h2>
          <p>
            문의: {CONTACT_EMAIL}
            <br />
            개인정보 처리에 관한 문의, 삭제 요청, 게시물 삭제 요청은 위 연락처로 접수해 주세요.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-[16px] font-bold">11. 고지의 의무</h2>
          <p>
            이 방침의 내용이 변경되는 경우 서비스 내 공지를 통해 알립니다. 이 방침은 위 시행일부터
            적용됩니다.
          </p>
        </section>
      </div>
    </div>
  );
}
