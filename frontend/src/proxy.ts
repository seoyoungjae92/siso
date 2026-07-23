import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ANON_ID_COOKIE = "anon_id";
const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

function adminAuthExpected(): string {
  const user = process.env.ADMIN_USERNAME ?? "admin";
  const pass = process.env.ADMIN_PASSWORD ?? "local-dev-only-password";
  return "Basic " + Buffer.from(`${user}:${pass}`).toString("base64");
}

export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/admin")) {
    const auth = request.headers.get("authorization");
    if (auth !== adminAuthExpected()) {
      return new NextResponse("Auth required", {
        status: 401,
        headers: { "WWW-Authenticate": 'Basic realm="admin"' },
      });
    }
  }

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
