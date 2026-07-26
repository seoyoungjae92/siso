import { AbuseSettingsForm } from "@/components/admin/AbuseSettingsForm";
import { CrawlSettingsForm } from "@/components/admin/CrawlSettingsForm";
import { ElectionSettingsForm } from "@/components/admin/ElectionSettingsForm";
import { ModerationSettingsForm } from "@/components/admin/ModerationSettingsForm";
import { PetitionSettingsForm } from "@/components/admin/PetitionSettingsForm";
import {
  fetchAbuseSettings,
  fetchCrawlSettings,
  fetchElectionSettings,
  fetchModerationSettings,
  fetchPetitionSettings,
  requireAdmin,
} from "@/lib/admin";

export default async function AdminSettingsPage() {
  await requireAdmin();
  const [crawlSettings, moderationSettings, abuseSettings, petitionSettings, electionSettings] =
    await Promise.all([
      fetchCrawlSettings(),
      fetchModerationSettings(),
      fetchAbuseSettings(),
      fetchPetitionSettings(),
      fetchElectionSettings(),
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

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">어뷰징 방지 설정</h2>

      {abuseSettings ? (
        <AbuseSettingsForm initial={abuseSettings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">국민청원 위젯 설정</h2>

      {petitionSettings ? (
        <PetitionSettingsForm initial={petitionSettings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}

      <h2 className="mb-4 mt-10 text-lg font-extrabold tracking-tight">선거 모드</h2>

      {electionSettings ? (
        <ElectionSettingsForm initial={electionSettings} />
      ) : (
        <p className="text-sm text-[#8A877E]">설정을 불러오지 못했습니다.</p>
      )}
    </div>
  );
}
