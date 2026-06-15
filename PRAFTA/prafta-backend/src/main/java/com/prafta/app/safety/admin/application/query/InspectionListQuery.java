package com.prafta.app.safety.admin.application.query;

/**
 * H1 순회점검 결과 리스트 매퍼 쿼리 (단일 사업장 스코프 + 월 필터).
 *
 * <p>siteCd 는 서비스가 권위 확정(요청 siteCd 또는 토큰 폴백) 후 멤버십 재검증한 값.
 *    workMonth(YYYYMM)는 점검일자(WORK_DATE) SUBSTR(1,6) 비교에 사용.
 */
public record InspectionListQuery(
      String gvCmpnyCd
    , String siteCd
    , String workMonth
) {
}
