import Link from "next/link";

import { ResolveFeedbackButton } from "@/components/admin/ResolveFeedbackButton";
import { fetchAdminFeedback, requireAdmin } from "@/lib/admin";

const CATEGORY_LABELS: Record<string, string> = {
  suggestion: "건의",
  report: "제보",
  bug: "버그 신고",
  etc: "기타",
};

const CATEGORY_FILTERS: { value: string | undefined; label: string }[] = [
  { value: undefined, label: "전체" },
  { value: "suggestion", label: "건의" },
  { value: "report", label: "제보" },
  { value: "bug", label: "버그 신고" },
  { value: "etc", label: "기타" },
];

function categoryHref(value: string | undefined) {
  return value ? `/admin/feedback?category=${value}` : "/admin/feedback";
}

export default async function AdminFeedbackPage({
  searchParams,
}: {
  searchParams: Promise<{ category?: string }>;
}) {
  await requireAdmin();
  const { category } = await searchParams;
  const items = await fetchAdminFeedback(category);

  const newItems = items.filter((f) => f.status === "new");
  const resolvedItems = items.filter((f) => f.status === "resolved");

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">제보/건의 관리</h1>

      <div className="mb-6 flex flex-wrap gap-2">
        {CATEGORY_FILTERS.map((f) => (
          <Link
            key={f.label}
            href={categoryHref(f.value)}
            className={`rounded-full px-3.5 py-1.5 text-xs font-bold ${
              category === f.value ? "bg-playground text-white" : "border border-line text-[#6B6960]"
            }`}
          >
            {f.label}
          </Link>
        ))}
      </div>

      <h2 className="mb-4 text-lg font-extrabold tracking-tight">신규 ({newItems.length})</h2>
      {newItems.length === 0 ? (
        <p className="text-sm text-[#6B6960]">신규 접수가 없습니다.</p>
      ) : (
        <div className="space-y-3">
          {newItems.map((f) => (
            <div key={f.id} className="rounded-xl border border-line bg-white p-4">
              <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
                <span className="font-semibold text-playground">{CATEGORY_LABELS[f.category] ?? f.category}</span>
                <span>{new Date(f.createdAt).toLocaleString("ko-KR")}</span>
              </div>
              <p className="mb-3 whitespace-pre-wrap text-[14px]">{f.body}</p>
              {f.contact && <p className="mb-3 text-xs text-[#8A877E]">연락처: {f.contact}</p>}
              <ResolveFeedbackButton id={f.id} />
            </div>
          ))}
        </div>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">처리완료 ({resolvedItems.length})</h2>
      {resolvedItems.length === 0 ? (
        <p className="text-sm text-[#6B6960]">처리완료된 항목이 없습니다.</p>
      ) : (
        <div className="space-y-3">
          {resolvedItems.map((f) => (
            <div key={f.id} className="rounded-xl border border-line bg-white p-4 opacity-60">
              <div className="mb-2 flex items-center justify-between text-xs text-[#8A877E]">
                <span className="font-semibold">{CATEGORY_LABELS[f.category] ?? f.category}</span>
                <span>{new Date(f.createdAt).toLocaleString("ko-KR")}</span>
              </div>
              <p className="whitespace-pre-wrap text-[14px]">{f.body}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
