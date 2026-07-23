import Link from "next/link";

export function Footer() {
  const appName = process.env.APP_NAME ?? "시소";

  return (
    <footer className="border-t border-line px-7 py-5 text-xs text-[#8A877E]">
      <nav className="mb-2 flex flex-wrap gap-x-4 gap-y-1">
        <Link href="/terms" className="font-semibold text-[#6B6960] hover:underline">
          이용약관
        </Link>
        <Link href="/privacy" className="font-semibold text-[#6B6960] hover:underline">
          개인정보처리방침
        </Link>
      </nav>
      <p>
        {appName}는 공개된 커뮤니티 게시물의 제목·요약·출처 링크만 수집해 병렬 비교합니다. 원문
        저작권은 각 원 커뮤니티 및 작성자에게 있습니다.
      </p>
      <p className="mt-1">© {appName}</p>
    </footer>
  );
}
