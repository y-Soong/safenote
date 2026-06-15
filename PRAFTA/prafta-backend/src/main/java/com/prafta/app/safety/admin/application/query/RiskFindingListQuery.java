package com.prafta.app.safety.admin.application.query;

/**
 * H3 위험성평가 목록 매퍼 쿼리 (사업장 스코프 강제 + 표시 필터).
 *
 * <p>siteCd 는 서비스가 권위 확정 후 멤버십 재검증한 값(항상 사업장 단위 강제).
 *    assessmentStatus/processCd/riskTypeCd 는 선택 필터(SYS011 상태는 param 에서 화이트리스트 검증됨).
 */
public record RiskFindingListQuery(
      String gvCmpnyCd
    , String siteCd
    , String assessmentStatus
    , String processCd
    , String riskTypeCd
) {
}
