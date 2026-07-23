"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/admin/dashboard", label: "대시보드" },
  { href: "/admin/reports", label: "신고 관리" },
  { href: "/admin/sources", label: "소스 관리" },
  { href: "/admin/settings", label: "크롤링 설정" },
];

export function AdminNav() {
  const pathname = usePathname();

  return (
    <nav className="border-b border-line bg-[#FBFAF7] px-4 py-3">
      <div className="mx-auto flex w-full max-w-3xl gap-4 text-sm">
        {LINKS.map((link) => {
          const active = pathname.startsWith(link.href);
          return (
            <Link
              key={link.href}
              href={link.href}
              className={active ? "font-extrabold text-playground" : "font-bold text-[#8A877E]"}
            >
              {link.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
