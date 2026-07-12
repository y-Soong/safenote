package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-030 후속: 일자범위 적용 초과근무 1건 (TB_USER_OVERTIME_MGMT).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectAppliedOvertimesByRange.
 *   출처/술어는 AppReq07Mapper.selectAppliedOvertimes(단일일)와 동일하되 WORK_YMD BETWEEN 범위로 일반화한다:
 *   DEL_YN='N' AND OT_STATUS&lt;&gt;'CANCELLED' AND ACTUAL_END_DATE/TIME IS NOT NULL(구간 확정분만).
 *
 * <p>일자별 그룹핑(서비스)을 위해 단일일 record 와 달리 workYmd 를 포함한다.
 *   구간 표현은 실제 시작/종료(ACTUAL_*) 일자·시각:
 *   startDate/startTime(NOT NULL), endDate/endTime(본 조회 술어상 NOT NULL), workMinutes(WORK_MINUTES, nullable).
 *
 * <p>★스키마 확인: TB_USER_OVERTIME_MGMT 에 WORK_SEQ 컬럼 없음 → 미포함(AppReq07Mapper 와 동일).
 *   매핑은 SELECT 컬럼 AS 별칭(camelCase) 기준(이름 기반).
 */
public record RangeOvertimeResult(
        String workYmd         // WORK_YMD YYYYMMDD
        , String startDate     // ACTUAL_START_DATE YYYYMMDD
        , String startTime     // ACTUAL_START_TIME HHmm
        , String endDate       // ACTUAL_END_DATE YYYYMMDD
        , String endTime       // ACTUAL_END_TIME HHmm
        , Integer workMinutes  // WORK_MINUTES (nullable)
) {
}
