import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ANON_ID_COOKIE = "anon_id";
const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

export function proxy(request: NextRequest) {
  if (request.cookies.has(ANON_ID_COOKIE)) {
    return NextResponse.next();
  }

  const response = NextResponse.next();
  response.cookies.set(ANON_ID_COOKIE, crypto.randomUUID(), {
    httpOnly: true,
    sameSite: "lax",
    maxAge: ONE_YEAR_SECONDS,
    path: "/",
  });
  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
