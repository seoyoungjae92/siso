export function BoxingGlovesIcon({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`inline-flex shrink-0 align-[-3px] ${className}`}
    >
      <span className="inline-block scale-x-[-1] [filter:hue-rotate(200deg)_saturate(1.8)]">🥊</span>
      <span className="-ml-1 inline-block">🥊</span>
    </span>
  );
}
