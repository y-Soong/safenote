package com.prafta.app.attd.attd01.result;

import java.math.BigDecimal;

/**
 * prafta-app-002: 사용자 연차 사용 실적 결과 (TB_USER_LEAVE_USE + TB_LEAVE_TYPE_MGMT).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectLeaveUseByRange.
 * <p>LEAVE_STATUS='CONFIRMED', DEL_YN='N' 만 조회. START_DATE~END_DATE 가 조회 범위와 겹치면 매칭.
 *   leaveNm 은 TB_LEAVE_TYPE_MGMT.LEAVE_NM 조인값.
 *
 * <p>prafta-app-018-E: 부분연차(시간차/반차) 상세 표시를 위해 단위/시각/차감분 컬럼 append.
 *   ⚠️ MyBatis 위치매핑 — 이 record 의 필드 순서는 mapper SELECT 컬럼 순서와 정확히 일치해야 한다.
 *   useUnitType=SYS025 코드(00종일/01반차/02 2h/03 1h/04 30분), startTime/endTime=HHMM(char),
 *   leaveMinutes=요청분(Integer). 기존 5필드 위치 불변, 그 뒤에 4개 추가.
 */
public record LeaveUseResult(
    String startDate
    , String endDate
    , String leaveCd
    , String leaveNm
    , BigDecimal leaveDays
    , String useUnitType
    , String startTime
    , String endTime
    , Integer leaveMinutes
) {
}
