export function BoxingGlovesIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 48 32"
      width="24"
      height="16"
      aria-hidden="true"
      className={`inline-block shrink-0 align-[-2px] ${className}`}
    >
      {/* 좌(파랑) 대 우(빨강) 복싱 글러브가 맞붙는 모티프 — "오늘의 링" 강조용.
          🥊 이모지처럼 주먹+엄지+손목 커프가 뚜렷이 보이는 형태로 그림. */}
      <g fill="#0047A0">
        <rect x="1" y="16" width="9" height="11" rx="2.5" />
        <path
          d="M6 18
             C4 12, 6 5, 13 4
             C19 3.5, 22 8, 21 13
             C20.5 17, 17 19, 12 19
             C8.5 19, 6.5 18.5, 6 18 Z"
        />
        <ellipse cx="19" cy="6.5" rx="4" ry="4.6" transform="rotate(-18 19 6.5)" />
      </g>
      <g stroke="#F5EFF6" strokeWidth="1.1" strokeLinecap="round">
        <line x1="2.5" y1="20" x2="8.5" y2="20" />
        <line x1="2.5" y1="23" x2="8.5" y2="23" />
      </g>

      <g fill="#CD2E3A">
        <rect x="38" y="16" width="9" height="11" rx="2.5" />
        <path
          d="M42 18
             C44 12, 42 5, 35 4
             C29 3.5, 26 8, 27 13
             C27.5 17, 31 19, 36 19
             C39.5 19, 41.5 18.5, 42 18 Z"
        />
        <ellipse cx="29" cy="6.5" rx="4" ry="4.6" transform="rotate(18 29 6.5)" />
      </g>
      <g stroke="#F5EFF6" strokeWidth="1.1" strokeLinecap="round">
        <line x1="39.5" y1="20" x2="45.5" y2="20" />
        <line x1="39.5" y1="23" x2="45.5" y2="23" />
      </g>
    </svg>
  );
}
