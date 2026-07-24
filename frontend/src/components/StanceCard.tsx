import { ACCENT } from "@/components/PostCard";
import type { Side } from "@/lib/posts";

export function StanceCard({ side, text }: { side: Side; text: string }) {
  return (
    <article className="rounded-[10px] border border-line bg-white p-3">
      <p className={`mb-1 text-[11px] font-bold ${ACCENT[side]}`}>{side === "left" ? "좌 시각" : "우 시각"}</p>
      <p className="text-[13px] text-[#6B6960]">{text}</p>
    </article>
  );
}
