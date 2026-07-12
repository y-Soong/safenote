package com.prafta.web.user.user01.result;

/**
 * 대상자의 현재/미래 부분(시간차) 연차 1건의 시간 정보 — PRAFTA-WEB_001-1.
 *
 * <p>불가케이스 ⑤(시간차 연차 미커버) 판정 대상. startTime/endTime 은 "HHmm" 4자리.
 * useUnitType 은 SYS025(00=종일/01=반차/02~04=시간차). 본 조회는 USE_UNIT_TYPE != '00'
 * 이면서 START_TIME 보유 행만 반환한다(시간대 보유 = 커버리지 판정 대상).
 */
public record PartialLeaveTimeResult(
    String leaveId
    , String startDate
    , String startTime
    , String endTime
    , String useUnitType
) {}
