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
 *
 * <p>PRAFTA_COM_002-B-1: 승인 대기(요청중) 연차 구분을 위해 연관 요청행 상태 reqStatus 를 맨 끝에 append.
 *   reqStatus=TB_USER_ATTD_REQ.REQ_STATUS(SYS033: 01신청/02승인/03반려/04취소). REQ_ID NULL(무결재 즉시확정)
 *   또는 조인 미매칭 시 null. "요청중" 파생 판정(REQ_ID NOT NULL AND REQ_STATUS='01')은 service 에서 수행.
 *
 * <p>작업지시서_연차변경화면_진입버튼: 연차 이동(TARGET_LEAVE_ID) 발의 식별자 leaveId 를 맨 끝에 append.
 *   leaveId=TB_USER_LEAVE_USE.LEAVE_ID. 이동 가능 여부(leaveMovable) 파생 계산은 service 에서 수행.
 *   ⚠️ 위치매핑이므로 반드시 마지막 필드로 두어 기존 10필드 순서를 유지한다.
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
    , String reqStatus
    , String leaveId
) {
}
