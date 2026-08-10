package com.prafta.app.leave.approval.result;

import java.math.BigDecimal;

/**
 * 사용자연차결재-01 (A-2 상세): 연차 본문(연차종류/유급여부/단위/구간) 1행 — selectLeaveBody 포팅.
 *
 * <p>paidYn 은 tb_leave_type_mgmt.PAID_TYPE(SYS023) 을 유급('Y')/무급('N') 으로 정규화한 값이다(F-PAID).
 */
public record LeaveDetailBodyRow(
      String leaveCd
    , String leaveNm
    , String paidYn
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    /**
     * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-01). 항상 0 이상(null 없음).
     * ★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼 borrowDays 와 순서 일치).
     */
    , BigDecimal borrowDays
) {
}
