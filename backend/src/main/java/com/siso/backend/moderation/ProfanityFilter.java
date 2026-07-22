package com.siso.backend.moderation;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 사전 기반 1차 필터(5절). 공백 제거 후 부분 문자열 매칭만 하는 단순한
 * 구현이라 자모 분리·특수문자 치환 같은 정교한 우회는 못 잡음 — 알려진
 * 한계이며, 더 정교한 탐지가 필요해지면 별도로 재검토할 것.
 */
@Component
public class ProfanityFilter {

    private static final Set<String> BANNED_WORDS = Set.of(
            "씨발", "씨팔", "시발", "개새끼", "새끼", "병신", "지랄",
            "좆", "존나", "닥쳐", "미친놈", "미친년", "걸레", "창녀",
            "죽여버려", "꺼져", "쓰레기같은", "찐따", "한남", "김치녀", "맘충");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public boolean containsBannedWord(String text) {
        String normalized = WHITESPACE.matcher(text).replaceAll("");
        return BANNED_WORDS.stream().anyMatch(normalized::contains);
    }
}
