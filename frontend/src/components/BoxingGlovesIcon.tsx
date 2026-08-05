export function BoxingGlovesIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 44 28"
      width="22"
      height="14"
      aria-hidden="true"
      className={`inline-block shrink-0 align-[-1px] ${className}`}
    >
      {/* 좌(파랑) 대 우(빨강) 글러브가 맞붙는 모티프 — "오늘의 링" 강조용 */}
      <g fill="#0047A0">
        <rect x="2" y="15" width="9" height="10" rx="3.5" />
        <rect x="8" y="7" width="13" height="14" rx="6.5" />
        <circle cx="18" cy="5.5" r="3.4" />
      </g>
      <g fill="#CD2E3A">
        <rect x="33" y="15" width="9" height="10" rx="3.5" />
        <rect x="23" y="7" width="13" height="14" rx="6.5" />
        <circle cx="26" cy="5.5" r="3.4" />
      </g>
      <path
        d="M22 10 L23.6 13.4 L27 15 L23.6 16.6 L22 20 L20.4 16.6 L17 15 L20.4 13.4 Z"
        fill="#F5EFF6"
      />
    </svg>
  );
}
