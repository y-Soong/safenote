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

    // PRAFTA-FIXEDOT-2(J13): 후방 고정연장(FROM/TO, HHMM, NULL=없음) — 전일 마지막 점유 구간이
    // 후방 고정연장이면 그 종료가 오버나이트 기준일 경계가 된다(지시서 지점 5).
    // 전방 고정연장은 전일 소정 시작 이전(당일 내 — V2)이라 이웃날 경계 판정과 무관 → 미조회.
    // ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑).
    , String fixedOtStrTime
    , String fixedOtEndTime
) {
}
