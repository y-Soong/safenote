package com.prafta.app.home.home01.result;

import java.math.BigDecimal;

/**
 * 연차 개편(표시): 홈 요약용 신청형 휴가('01') 집계 결과(간결 — 타입 수 + 총잔여).
 *
 * <p>법정/관리자부여(GRANT 그룹, selectLeaveSummary)와 절대 합산하지 않는 별도 산출.
 *   타입별 한도/사용 상세는 연차 현황 화면(leave01.selectAppliedLeaveTypes)에서 제공하고,
 *   홈은 요약이므로 보유 타입 수와 총잔여 합만 노출한다.
 *
 * <p>매핑(AppHome01Mapper.selectAppliedLeaveSummary):
 * <pre>
 *   COUNT(*)                                                  AS typeCount
 *   SUM( GREATEST(IFNULL(MAX_APLY_DAYS,0) - 회계연도 사용분, 0) ) AS totalRemainDays
 * </pre>
 * '01' 타입이 0개면 typeCount=0, totalRemainDays=null(서비스에서 0.0 폴백).
 */
public record AppliedLeaveSummaryResult(
    int typeCount
    , BigDecimal totalRemainDays
) {
}
