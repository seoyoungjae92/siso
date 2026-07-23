"use server";

import { fetchPairs } from "@/lib/pairs";
import { fetchPosts } from "@/lib/posts";
import type { Side } from "@/lib/posts";

export async function loadMorePosts(side: Side, page: number) {
  return fetchPosts(side, page);
}

export async function loadMorePairs(page: number) {
  return fetchPairs(page);
}
