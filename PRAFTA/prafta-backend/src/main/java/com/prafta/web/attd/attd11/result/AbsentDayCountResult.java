package com.prafta.web.attd.attd11.result;

/**
 * PRAFTA-COM-016-F (8-3) - Attd_11 사용자별 미출근일 수 집계 결과.
 *
 * 미출근 = "스케줄(WORK_PLAN_CD=SCH_CD)이 있으나 출근하지 않은 날 수".
 * 휴일(TB_HOLIDAY)·연차(TB_USER_LEAVE_USE 종일 확정)·출근기록 존재일·미래일은 제외.
 * 사용자별 COUNT(distinct WORK_YMD). selectAttdSummaryRows 와 동일 스코프(node_tree+target_user).
 */
public record AbsentDayCountResult(
        String userCd
        , int absentDayCnt
) {
}
