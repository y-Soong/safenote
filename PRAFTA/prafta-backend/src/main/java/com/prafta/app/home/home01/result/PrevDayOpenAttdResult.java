package com.prafta.app.home.home01.result;

/**
 * prafta-app-021: 메인 홈 "직전일(today-1) 미퇴근" 근태 1건 조회 결과 (TB_USER_ATTD_MGMT).
 *
 * <p>열린 근태 = DEL_YN='N' && CHECK_IN_TIME 有 && CHECK_OUT_TIME NULL, WORK_YMD = today-1.
 *   존재하면 메인 퇴근 버튼을 활성화하고(canCheckOut), 전날 출근시각을 카드에 안내한다(§7.6).
 *
 * <pre>
 *   WORK_YMD      AS workYmd      (varchar8 YYYYMMDD)
 *   WORK_SEQ      AS workSeq
 *   CHECK_IN_TIME AS checkInTime  (varchar4 HHMM)
 * </pre>
 */
public record PrevDayOpenAttdResult(
    String workYmd
    , Integer workSeq
    , String checkInTime
) {
}
