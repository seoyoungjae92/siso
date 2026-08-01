import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center px-4 py-20 text-center">
      <h1 className="mb-2 text-2xl font-extrabold tracking-tight">페이지를 찾을 수 없습니다</h1>
      <p className="mb-6 text-sm text-[#6B6960]">주소가 잘못됐거나, 삭제된 페이지일 수 있습니다.</p>
      <Link href="/" className="rounded-full bg-playground px-5 py-2 text-sm font-bold text-white">
        홈으로 돌아가기
      </Link>
    </div>
  );
}
