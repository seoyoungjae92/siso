import { cookies } from "next/headers";

const ANON_ID_COOKIE = "anon_id";

export async function getAnonId(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(ANON_ID_COOKIE)?.value;
}
