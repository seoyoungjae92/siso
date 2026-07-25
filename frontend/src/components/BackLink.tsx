"use client";

import { useRouter } from "next/navigation";

export function BackLink() {
  const router = useRouter();

  function handleClick() {
    // 북마크/새 탭 등으로 직접 들어와 뒤로 갈 히스토리가 없으면 back()이
    // 아무 동작도 안 하거나 사이트 밖으로 나가버릴 수 있어 — 이 탭에
    // 쌓인 히스토리가 있을 때만 back(), 없으면 홈으로 보낸다.
    if (window.history.length > 1) {
      router.back();
    } else {
      router.push("/");
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className="mb-4 inline-block text-[13px] font-bold text-[#6B6960]"
    >
      ← 이전 페이지로
    </button>
  );
}
