import { CrawlSettingsForm } from "@/components/admin/CrawlSettingsForm";
import { fetchCrawlSettings, requireAdmin } from "@/lib/admin";

export default async function AdminSettingsPage() {
  await requireAdmin();
  const settings = await fetchCrawlSettings();

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">크롤링 설정</h1>

      {settings ? (
        <CrawlSettingsForm initial={settings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}
    </div>
  );
}
