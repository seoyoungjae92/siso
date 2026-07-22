export function Header() {
  const appName = process.env.APP_NAME ?? "siso";

  return (
    <header className="flex items-center justify-between border-b border-line bg-white px-7 py-3.5">
      <div className="flex items-center gap-2.5 text-xl font-extrabold tracking-tight">
        <span className="relative block h-[26px] w-[26px] overflow-hidden rounded-full bg-gradient-to-b from-right-red from-50% to-left-blue to-50%" />
        {appName}
        <small className="text-[11px] font-medium text-[#8A877E]">같은 주제, 다른 시선</small>
      </div>
    </header>
  );
}
