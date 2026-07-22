package com.siso.backend.anon;

import java.util.List;

public final class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "떠도는", "조용한", "수줍은", "용감한", "재빠른", "느긋한", "엉뚱한", "새침한",
            "씩씩한", "나른한", "든든한", "유쾌한", "냉철한", "다정한", "무심한", "은밀한",
            "활발한", "고요한", "날카로운", "포근한", "우직한", "담대한", "신중한", "명랑한");

    private static final List<String> NOUNS = List.of(
            "두루미", "여우", "고양이", "부엉이", "다람쥐", "고래", "늑대", "참새",
            "거북이", "수달", "까치", "호랑이", "사슴", "올빼미", "펭귄", "낙타",
            "코끼리", "물고기", "나비", "벌새", "반딧불", "은하수", "파도", "구름");

    private NicknameGenerator() {
    }

    public static String generate(String anonId) {
        int hash = anonId.hashCode();
        String adjective = ADJECTIVES.get(Math.floorMod(hash, ADJECTIVES.size()));
        String noun = NOUNS.get(Math.floorMod(hash / ADJECTIVES.size(), NOUNS.size()));
        String suffix = anonId.replace("-", "");
        suffix = suffix.substring(0, Math.min(4, suffix.length()));
        return adjective + " " + noun + " " + suffix;
    }
}
