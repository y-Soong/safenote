package com.prafta.app.safety.admin.result;

/**
 * 위험성평가 상태전환 가드용 현재 상태 단건 VO (PK + 현재 진행상태).
 *
 * <p>사업장 스코프(CMPNY+SITE+PROCESS+ASSESSMENT) 내 존재 여부 확인 + 현재 상태(전이 검증) 용도.
 *    없으면 매퍼가 null 반환 → 서비스가 404(IDOR 차단).
 */
public record RiskStatusMetaResult(
    String cmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , String assessmentStatus
){
}
