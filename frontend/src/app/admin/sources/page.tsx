import { NewSourceForm } from "@/components/admin/NewSourceForm";
import { SourceRow } from "@/components/admin/SourceRow";
import { fetchSources, requireAdmin } from "@/lib/admin";

export default async function AdminSourcesPage() {
  await requireAdmin();
  const sources = await fetchSources();

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight">소스 관리</h1>

      <div className="mb-6">
        <NewSourceForm />
      </div>

      <div className="space-y-3">
        {sources.map((source) => (
          <SourceRow key={source.id} source={source} />
        ))}
      </div>
    </div>
  );
}
