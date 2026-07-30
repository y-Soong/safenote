package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * 그날 잔존 시간차(02/03/04) 사용행 — 취소·반려 재정산 대상 (LC-05, F1 → PC-01 REQ 묶음 재작업).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * @param leaveId      사용기록 ID
 * @param reqId        신청 요청 ID — PC-01: 분할 INSERT(한 신청=여러 행) 묶음 키. 직접 차감 등은 null.
 * @param grantId      차감 부여 ID('01' 사용자 신청 타입은 null)
 * @param leaveMinutes 신청 분(시간차는 병기 저장 — 단, 분할 INSERT 시 <b>첫 행만</b> 보유(N1).
 *                     REQ 묶음 합산이 곧 신청 분이다)
 * @param leaveDays    현재 저장된 차감 일수(재정산 비교 대상)
 * @param startTime    시작 시각(HHMM) — 시간순 재적용 정렬 키(지시서 F1)
 * @param availToDate  차감 부여의 만료일(YYYYMMDD) — PC-01 묶음 내 배분(만료 임박순) 정렬 키.
 *                     GRANT 미연결 행은 null(배분 최후순 취급)
 */
public record HourlyLeaveUseRowVO(
      String leaveId
    , String reqId
    , String grantId
    , Integer leaveMinutes
    , BigDecimal leaveDays
    , String startTime
    , String availToDate
) {
}
