export default function Loading() {
  return (
    <div className="flex flex-1 items-center justify-center py-20">
      <div
        role="status"
        aria-label="불러오는 중"
        className="h-8 w-8 animate-spin rounded-full border-[3px] border-line border-t-playground"
      />
    </div>
  );
}
