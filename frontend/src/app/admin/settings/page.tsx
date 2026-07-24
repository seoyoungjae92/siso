import { CrawlSettingsForm } from "@/components/admin/CrawlSettingsForm";
import { ModerationSettingsForm } from "@/components/admin/ModerationSettingsForm";
import { fetchCrawlSettings, fetchModerationSettings, requireAdmin } from "@/lib/admin";

export default async function AdminSettingsPage() {
  await requireAdmin();
  const [crawlSettings, moderationSettings] = await Promise.all([
    fetchCrawlSettings(),
    fetchModerationSettings(),
  ]);

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">크롤링 설정</h1>

      {crawlSettings ? (
        <CrawlSettingsForm initial={crawlSettings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">모더레이션 설정</h2>

      {moderationSettings ? (
        <ModerationSettingsForm initial={moderationSettings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}
    </div>
  );
}
