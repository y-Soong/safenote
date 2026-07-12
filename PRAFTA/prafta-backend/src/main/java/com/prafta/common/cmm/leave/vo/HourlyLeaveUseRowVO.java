package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * 그날 잔존 시간차(02/03/04) 사용행 — 취소·반려 재정산 대상 (LC-05, F1).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * @param leaveId      사용기록 ID
 * @param grantId      차감 부여 ID('01' 사용자 신청 타입은 null)
 * @param leaveMinutes 신청 분(시간차는 항상 병기 저장 — 재정산 원본)
 * @param leaveDays    현재 저장된 차감 일수(재정산 비교 대상)
 * @param startTime    시작 시각(HHMM) — 시간순 재적용 정렬 키(지시서 F1)
 */
public record HourlyLeaveUseRowVO(
      String leaveId
    , String grantId
    , Integer leaveMinutes
    , BigDecimal leaveDays
    , String startTime
) {
}
