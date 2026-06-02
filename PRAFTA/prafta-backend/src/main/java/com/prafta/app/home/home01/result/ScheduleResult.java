package com.prafta.app.home.home01.result;

/**
 * prafta-app-001: 오늘 예정 스케줄 조회 결과
 * (tb_user_work_plan → WORK_PLAN_CD(=SCH_CD) → tb_sch_mgmt).
 * <p>매핑(AppHome01Mapper.selectTodaySchedule):
 * <pre>
 *   FST_SCH_STR_TIME AS scheduleStart    (varchar4 HHMM)
 *   FST_SCH_END_TIME AS scheduleEnd      (varchar4 HHMM)
 *   SEC_SCH_STR_TIME AS secScheduleStart (varchar4 HHMM, null 허용 — 2구간 스케줄만)
 *   SEC_SCH_END_TIME AS secScheduleEnd   (varchar4 HHMM, null 허용)
 * </pre>
 * work_plan 이 LEAVE_CD(연차/휴무) 거나 매칭 스케줄이 없으면 결과 없음 → start/end=null.
 * <p>secScheduleStart 가 채워져 있으면 2구간 스케줄(MainView 출퇴근 카드의 구간 전환 판정에 사용).
 */
public record ScheduleResult(
    String scheduleStart
    , String scheduleEnd
    , String secScheduleStart
    , String secScheduleEnd
) {
}
