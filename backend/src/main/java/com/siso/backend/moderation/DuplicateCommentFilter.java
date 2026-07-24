package com.siso.backend.moderation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 편집 거리(Levenshtein) 기반 유사도 검사. commons-lang3의
 * StringUtils.getLevenshteinDistance는 deprecated라 직접 구현한다.
 */
@Component
public class DuplicateCommentFilter {

    public boolean isDuplicate(String newBody, List<String> recentBodies, double threshold) {
        return recentBodies.stream().anyMatch(recent -> similarity(newBody, recent) >= threshold);
    }

    private double similarity(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshtein(a, b) / maxLen);
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[b.length()];
    }
}
