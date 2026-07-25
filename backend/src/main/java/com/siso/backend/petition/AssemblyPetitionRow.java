package com.siso.backend.petition;

/**
 * 국회 열린국회정보 Open API(PTTRCP, 청원 접수목록)의 원본 응답 필드를 그대로
 * 옮긴 값 — CITZN_AGM_CNT는 "53,728" 같은 콤마 포함 문자열이고 의원소개
 * 청원(PTT_KIND="의원소개")은 null이라 파싱은 PetitionService에서 처리.
 */
public record AssemblyPetitionRow(
        String pttId, String title, String kind, String agreeCountRaw, String receivedAt, String linkUrl) {
}
