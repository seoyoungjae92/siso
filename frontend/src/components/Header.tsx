function SeesawLogo() {
  return (
    <svg viewBox="0 0 40 21" width="26" height="26" aria-hidden="true">
      {/* 받침대 다리 */}
      <path d="M20 12.4 L25 19.5 L15 19.5 Z" fill="#1B1B22" />
      {/* 평평한(180도) 판 — 좌우 대칭이 흔들리지 않는 상태를 표현 */}
      <rect x="3" y="10" width="34" height="2.4" rx="1.2" fill="#1B1B22" />
      {/* 왼쪽 의자 (파랑) */}
      <rect x="2" y="4" width="7" height="7" rx="1.6" fill="#0047A0" />
      {/* 오른쪽 의자 (빨강) */}
      <rect x="31" y="4" width="7" height="7" rx="1.6" fill="#CD2E3A" />
      {/* 가운데 태극 축 — 실제 태극기와 동일한 비율(바깥 반원 + 안쪽 반지름
          1/2 반원 2개)의 S자 곡선. 기본 path는 좌우(빨강 오른쪽)로 나와서
          -90도 회전해 태극기처럼 빨강 위/파랑 아래로 맞춤 */}
      <g transform="rotate(-90 20 11.2)">
        <circle cx="20" cy="11.2" r="4.4" fill="#0047A0" />
        <path
          d="M20,6.8 A4.4,4.4 0 0,1 20,15.6 A2.2,2.2 0 0,0 20,11.2 A2.2,2.2 0 0,1 20,6.8 Z"
          fill="#CD2E3A"
        />
      </g>
    </svg>
  );
}

export function Header() {
  const appName = process.env.APP_NAME ?? "시소";

  return (
    <header className="flex items-center justify-between border-b border-line bg-white px-7 py-3.5">
      <div className="flex items-center gap-2.5 text-xl font-extrabold tracking-tight">
        <SeesawLogo />
        {appName}
        <small className="text-[11px] font-medium text-[#8A877E]">같은 주제, 다른 시선</small>
      </div>
    </header>
  );
}
