package com.prafta.web.attd.leaveflow.vo;

import java.math.BigDecimal;

/**
 * 연차 수정('06') 승인 시 대상 사용기록(TB_USER_LEAVE_USE)과 그 차감 부여(TB_USER_LEAVE_GRANT)
 * 잔여를 한 번에 싣는 VO (PRAFTA-025).
 *
 * <p>{@code leaveDays}는 수정 전(기존) 차감 일수이며, 잔여 가드 계산 시
 * "본 건 차감을 되돌린 가용분"을 구하는 데 쓴다:
 * availableExclSelf = grantDays - usedDays + leaveDays.
 */
public record LeaveModifyTargetVO(
      String leaveCd
    , String grantId
    , String useUnitType
    , BigDecimal leaveDays
    , String siteCd
    , String userCd
    , String startDate
    , BigDecimal grantDays
    , BigDecimal usedDays
) {
}
