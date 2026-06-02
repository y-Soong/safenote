package com.prafta.app.attd.attd01.result;

import java.math.BigDecimal;

/**
 * prafta-app-002: 사용자 연차 사용 실적 결과 (TB_USER_LEAVE_USE + TB_LEAVE_TYPE_MGMT).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectLeaveUseByRange.
 * <p>LEAVE_STATUS='CONFIRMED', DEL_YN='N' 만 조회. START_DATE~END_DATE 가 조회 범위와 겹치면 매칭.
 *   leaveNm 은 TB_LEAVE_TYPE_MGMT.LEAVE_NM 조인값.
 */
public record LeaveUseResult(
    String startDate
    , String endDate
    , String leaveCd
    , String leaveNm
    , BigDecimal leaveDays
) {
}
