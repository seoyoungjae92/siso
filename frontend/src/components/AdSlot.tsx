const LABELS: Record<string, string> = {
  "feed-left": "AD · 좌 피드 슬롯",
  "feed-right": "AD · 우 피드 슬롯",
  playground: "AD · 플레이그라운드 슬롯",
  discussion: "AD · 토론 페이지 슬롯",
};

export function AdSlot({ position }: { position: keyof typeof LABELS }) {
  return (
    <div className="mb-2.5 flex h-24 items-center justify-center rounded-[10px] border-[1.5px] border-dashed border-[#BDBAB0] bg-[#FBFAF7] text-xs font-bold tracking-widest text-[#A09D94]">
      {LABELS[position]}
    </div>
  );
}
