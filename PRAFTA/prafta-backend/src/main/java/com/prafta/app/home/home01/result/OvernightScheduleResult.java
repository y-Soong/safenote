package com.prafta.app.home.home01.result;

/**
 * prafta-app-013-1: 전일 스케줄 종료시각 산출 결과(오버나이트 기준일 판정용).
 *
 * <p>매핑 대상: AppHome01Mapper.selectScheduleEndForOvernight
 *   (TB_USER_WORK_PLAN(전일 WORK_YMD) → TB_SCH_MGMT INNER JOIN).
 * <p>시간 컬럼은 모두 varchar(4) HHMM. 2구간 여부는 secSchStrTime 이 not null 인지로 판정한다.
 *   WORK_PLAN_CD 가 null(휴무/미배정)이면 JOIN 결과 없음 → 본 record 자체가 null 로 반환된다.
 */
public record OvernightScheduleResult(
    String fstSchStrTime
    , String fstSchEndTime
    , String secSchStrTime
    , String secSchEndTime
) {
}
