package com.prafta.app.leave.leaveflow.result;

/**
 * prafta-app-018-B: 연차 종류 메타(결재여부·허용단위 산출 입력).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO} 미러.
 * ⚠️ MyBatis record 위치매핑 — SELECT 컬럼 순서(systemYn, aprvUseYn, useUnitType)와 일치해야 한다.
 */
public record LeaveTypeInfoRow(
      String systemYn
    , String aprvUseYn
    , String useUnitType
) {
}
