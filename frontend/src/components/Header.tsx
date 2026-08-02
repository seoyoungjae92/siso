import Link from "next/link";

function SeesawLogo() {
  return (
    <svg viewBox="0 0 40 40" width="26" height="26" aria-hidden="true">
      {/* 받침대 다리 */}
      <path d="M20 20 L12 34 L28 34 Z" fill="#1B1B22" />
      {/* 평평한(수평) 판 — 좌우 대칭이 흔들리지 않는 상태를 표현 */}
      <rect x="3" y="16.5" width="34" height="3" rx="1.5" fill="#1B1B22" />
      {/* 왼쪽 의자 (파랑) — 동그란 형태로 좀 더 귀엽게 */}
      <circle cx="8" cy="11" r="6.5" fill="#0047A0" />
      {/* 오른쪽 의자 (빨강) */}
      <circle cx="32" cy="11" r="6.5" fill="#CD2E3A" />
      {/* 가운데 태극 축 — 실제 태극기와 동일한 비율(바깥 반원 + 안쪽 반지름
          1/2 반원 2개)의 S자 곡선. 기본 path는 좌우(빨강 오른쪽)로 나와서
          -90도 회전해 태극기처럼 빨강 위/파랑 아래로 맞춤 */}
      <g transform="rotate(-90 20 18)">
        <circle cx="20" cy="18" r="3.6" fill="#0047A0" />
        <path
          d="M20,14.4 A3.6,3.6 0 0,1 20,21.6 A1.8,1.8 0 0,0 20,18 A1.8,1.8 0 0,1 20,14.4 Z"
          fill="#CD2E3A"
        />
      </g>
    </svg>
  );
}

export function Header() {
  const appName = "시소";

  return (
    <header className="sticky top-0 z-40 flex items-center justify-between gap-2 border-b border-line bg-white px-4 py-3.5 sm:px-7">
      <Link href="/" className="flex items-center gap-2 text-lg font-extrabold tracking-tight sm:gap-2.5 sm:text-xl">
        <SeesawLogo />
        {appName}
        <small className="hidden text-[11px] font-medium text-[#8A877E] sm:inline">같은 주제, 다른 시선</small>
      </Link>
      <nav className="flex items-center gap-2 text-[11px] font-semibold whitespace-nowrap text-[#8A877E] sm:gap-3">
        <Link href="/feedback" className="hover:text-[#6B6960] hover:underline">
          <span className="sm:hidden">제보</span>
          <span className="hidden sm:inline">제보·건의하기</span>
        </Link>
        <Link href="/terms" className="hover:text-[#6B6960] hover:underline">
          <span className="sm:hidden">약관</span>
          <span className="hidden sm:inline">이용약관</span>
        </Link>
        <Link href="/privacy" className="hover:text-[#6B6960] hover:underline">
          <span className="sm:hidden">개인정보</span>
          <span className="hidden sm:inline">개인정보처리방침</span>
        </Link>
      </nav>
    </header>
  );
}
