package com.prafta.app.leave.leave01.result;

import java.math.BigDecimal;

/**
 * 앱 "연차 현황" 사용 내역 1행 (TB_USER_LEAVE_USE CONFIRMED ⨝ TB_LEAVE_TYPE_MGMT).
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서와 생성자 인자 순서가 일치해야 한다
 * (AppLeave01Mapper.selectLeaveUsesByRange 의 SELECT 순서 변경 시 함께 수정할 것).
 */
public record LeaveUseHistoryRow(
      String leaveId
    , String startDate      // 사용 시작일 YYYYMMDD
    , String endDate        // 사용 종료일 YYYYMMDD
    , String startTime      // 시작 시각 HHMM (시간단위 휴가만, 그 외 null)
    , String endTime        // 종료 시각 HHMM (시간단위 휴가만, 그 외 null)
    , String useUnitType    // 사용 단위 SYS025 (00:1일 / 01:반차 / 02~04:시간차 / 05:반반차)
    , BigDecimal leaveDays  // 차감 일수 (시간차는 동적 환산값)
    , Integer leaveMinutes  // 사용 분 (시간단위 휴가만, 그 외 null)
    , String leaveNm        // 연차 종류명 (타입 삭제 시 null 가능 — FE 폴백 표기)
) {
}
