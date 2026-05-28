package com.prafta.web.attd.leaveflow.vo;

/**
 * 요청 연결 확정 사용의 핵심 정보 (prafta-019-E 후속 — 출근차단/finalize용).
 */
public record LeaveUseDetailVO(
      String leaveCd
    , String useUnitType
    , String userCd
    , String siteCd
    , String startDate
) {
}
