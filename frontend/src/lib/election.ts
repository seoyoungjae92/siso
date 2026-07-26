import { BACKEND_API_URL } from "@/lib/posts";

export async function fetchElectionMode(): Promise<boolean> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/election-mode`, { cache: "no-store" });
    if (!res.ok) return false;
    const data: { enabled: boolean } = await res.json();
    return data.enabled;
  } catch {
    return false;
  }
}
