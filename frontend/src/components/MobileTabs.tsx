"use client";

import { useState } from "react";

import { FeedColumn } from "@/components/FeedColumn";
import { Playground } from "@/components/Playground";
import type { TopicPair } from "@/lib/pairs";
import type { PostSummary } from "@/lib/posts";

type Tab = "left" | "playground" | "right";

const TAB_CONFIG: Record<Tab, { label: string; text: string; bar: string }> = {
  left: { label: "좌", text: "text-left-blue", bar: "bg-left-blue" },
  playground: { label: "플레이그라운드", text: "text-playground", bar: "bg-playground" },
  right: { label: "우", text: "text-right-red", bar: "bg-right-red" },
};

const TABS: Tab[] = ["left", "playground", "right"];

export function MobileTabs({
  leftPosts,
  rightPosts,
  pairs,
}: {
  leftPosts: PostSummary[];
  rightPosts: PostSummary[];
  pairs: TopicPair[];
}) {
  const [tab, setTab] = useState<Tab>("playground");

  return (
    <div className="flex flex-1 flex-col">
      <div className="flex-1 overflow-y-auto">
        {tab === "left" && <FeedColumn side="left" posts={leftPosts} />}
        {tab === "playground" && (
          <div className="bg-pg-tint px-[14px] py-4">
            <Playground pairs={pairs} />
          </div>
        )}
        {tab === "right" && <FeedColumn side="right" posts={rightPosts} />}
      </div>

      <div className="sticky bottom-0 grid grid-cols-3 border-t border-line bg-white">
        {TABS.map((t) => {
          const config = TAB_CONFIG[t];
          const active = t === tab;
          return (
            <button
              key={t}
              type="button"
              onClick={() => setTab(t)}
              className={`relative py-2.5 pb-3.5 text-[11px] font-extrabold ${active ? config.text : "text-[#A09D94]"}`}
            >
              {active && (
                <span className={`absolute inset-x-[20%] top-0 h-[3px] rounded-b ${config.bar}`} />
              )}
              {config.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
