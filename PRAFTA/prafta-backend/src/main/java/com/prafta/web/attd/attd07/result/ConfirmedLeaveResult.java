package com.prafta.web.attd.attd07.result;

/**
 * Result row of {@code Attd07Mapper.selectDailyConfirmedLeave}.
 *
 * PRAFTA-APP-018-F: 일자 상세 팝업의 "연차 사용" 섹션(표시 전용)에 쓰이는 그날 확정 연차 1건.
 *   TB_USER_LEAVE_USE(LEAVE_STATUS='CONFIRMED', DEL_YN='N') 행을 일자(START_DATE) 단위로 1:1 매핑한다.
 *   결재형 연차도 leave_use 는 즉시 CONFIRMED 라 미처리(01) 결재대기분은 D 의 요청 카드가 소유하므로
 *   매퍼 WHERE 에서 제외한다(이중표시 방지).
 *
 * ⚠️ MyBatis record 는 컬럼을 "순서(위치)"로 생성자 인자에 바인딩한다.
 *    아래 필드 순서는 selectDailyConfirmedLeave 의 SELECT 절 컬럼 순서와 정확히 일치해야 한다
 *    (record 끝 = SELECT 끝). 중간 삽입/순서 변경 금지(전 필드 밀림 → 런타임 변환 폭발).
 */
public record ConfirmedLeaveResult(
    /** 연차 종류 코드 (TB_USER_LEAVE_USE.LEAVE_CD) */
      String leaveCd

    /** 연차 종류명 (TB_LEAVE_TYPE_MGMT.LEAVE_NM, 예 "월차") */
    , String leaveNm

    /** 사용단위 코드 [SYS025] (00종일/01반차/02 2시간/03 1시간/04 30분) */
    , String useUnitType

    /** 사용단위 한글 라벨 (SYS025 FNC 산출: 종일/반차/2시간/1시간/30분) */
    , String unitNm

    /** 시작 시각 (HHmm, 시간차일 때만 의미) */
    , String startTime

    /** 종료 시각 (HHmm, 시간차일 때만 의미) */
    , String endTime

    /** 차감 일수 (decimal 문자열 그대로 — FE 가 정규화) */
    , String leaveDays
) {
}
