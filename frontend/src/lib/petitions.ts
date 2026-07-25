import { BACKEND_API_URL } from "@/lib/posts";

export type Petition = {
  id: string;
  title: string;
  agreeCount: number;
  receivedAt: string;
  linkUrl: string;
};

export async function fetchTopPetitions(): Promise<Petition[]> {
  try {
    const res = await fetch(`${BACKEND_API_URL}/api/petitions/top`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return [];
    }

    return res.json();
  } catch {
    return [];
  }
}
