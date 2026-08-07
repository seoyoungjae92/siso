import { Analytics } from "@vercel/analytics/next";
import type { Metadata } from "next";
import Script from "next/script";

import { BackToTopButton } from "@/components/BackToTopButton";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";

import "./globals.css";

const appName = "시소";

// og:image 등 메타데이터의 절대 URL을 만드는 기준 — 없으면 Next.js가
// http://localhost:3000으로 fallback해서, 카톡/문자 링크 미리보기 서버가
// 절대 못 가져오는 주소가 og:image에 그대로 박힌다(실측으로 확인한 버그).
// 서비스 도메인 미정(CLAUDE.md D7)이라 환경변수로 추상화.
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

const tagline = "같은 주제, 다른 시선";
const description = "좌·우 커뮤니티 글을 나란히 모아보고, 같은 주제를 두고 익명으로 토론하는 공간";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: `${appName} — ${tagline}`,
    template: `%s — ${appName}`,
  },
  description,
  openGraph: {
    title: `${appName} — ${tagline}`,
    description,
    siteName: appName,
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: `${appName} — ${tagline}`,
    description,
  },
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
        {/* 애드센스 사이트 소유 확인 겸 광고 로딩 스크립트 — 심사 중에도
            필요하고, 승인되면 그대로 광고 서빙에도 쓰임(별도 코드 불필요) */}
        <Script
          async
          src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-6044926397054590"
          crossOrigin="anonymous"
          strategy="afterInteractive"
        />
        {/* reCAPTCHA(Enterprise Assessment API, 2024년부터 신규 키가 이 방식으로만
            발급됨) — 사이트 키 미설정 시(계정 발급 전) 스크립트 자체를 안 실어서
            CommentForm의 grecaptcha.enterprise 호출도 자연히 스킵된다 */}
        {process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY && (
          <Script
            async
            src={`https://www.google.com/recaptcha/enterprise.js?render=${process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY}`}
            strategy="afterInteractive"
          />
        )}
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
