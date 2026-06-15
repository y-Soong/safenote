package com.prafta.app.safety.admin.application.query;

/**
 * H4 위험성평가 상세/메타 매퍼 쿼리 (PK 4키 + 사업장 스코프).
 *
 * <p>CMPNY+SITE+PROCESS+ASSESSMENT 4키 단건. 스코프 밖이면 매퍼가 null 반환 → 404(IDOR 차단).
 */
public record RiskDetailQuery(
      String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
) {
}
