package com.siso.backend.petition;

/**
 * PTTINFODETAIL(청원 상세정보) 응답 중 마감 판정에 필요한 필드만.
 * committeeReferredAt(JRCMIT_CMMT_DT, 소관위원회 회부일)이 null이 아니면
 * 국민동의청원이 5만 동의를 채워 위원회에 회부됐다는 뜻 — 이걸 "성립"
 * 판정 근거로 쓴다(실제 API 필드에 별도 성립/미성립 플래그가 없음을
 * 실측으로 확인).
 */
public record AssemblyPetitionDetail(
        String pttId, String committeeName, String committeeReferredAt, String achvRatioRaw) {
}
