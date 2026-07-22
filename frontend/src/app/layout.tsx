import type { Metadata } from "next";
import "./globals.css";

const appName = process.env.APP_NAME ?? "시소";

export const metadata: Metadata = {
  title: appName,
  description: "좌·우 커뮤니티 모아보기 + 익명 토론 플레이그라운드",
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
      <body className="min-h-full flex flex-col font-sans">{children}</body>
    </html>
  );
}
