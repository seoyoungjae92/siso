import { Analytics } from "@vercel/analytics/next";
import type { Metadata } from "next";

import { BackToTopButton } from "@/components/BackToTopButton";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";

import "./globals.css";

const appName = process.env.APP_NAME ?? "시소";

// og:image 등 메타데이터의 절대 URL을 만드는 기준 — 없으면 Next.js가
// http://localhost:3000으로 fallback해서, 카톡/문자 링크 미리보기 서버가
// 절대 못 가져오는 주소가 og:image에 그대로 박힌다(실측으로 확인한 버그).
// 서비스 도메인 미정(CLAUDE.md D7)이라 환경변수로 추상화.
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: appName,
  description: "좌·우 커뮤니티 모아보기 + 익명 토론 놀이터",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full antialiased">
      <head>
        <link
          rel="stylesheet"
          as="style"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.css"
        />
      </head>
      <body className="min-h-full flex flex-col font-sans">
        <Header />
        {children}
        <Footer />
        <BackToTopButton />
        <Analytics />
      </body>
    </html>
  );
}
