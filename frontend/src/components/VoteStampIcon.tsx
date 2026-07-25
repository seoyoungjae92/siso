export function VoteStampIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 20 20"
      width="14"
      height="14"
      aria-hidden="true"
      className={`inline-block shrink-0 align-[-2px] ${className}`}
    >
      {/* 대한민국 투표 기표 도장 모티프 — 배경은 투명하게 두고(카드가 이미
          흰 배경) 빨간 도장 자국만 표현. 원 도장 안에 "卜" 모양 표식 */}
      <circle cx="10" cy="10" r="6.5" fill="none" stroke="#CD2E3A" strokeWidth="1.6" />
      <path
        d="M10 5.8 V13.2 M10 9 L12.8 7.2"
        stroke="#CD2E3A"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  );
}
