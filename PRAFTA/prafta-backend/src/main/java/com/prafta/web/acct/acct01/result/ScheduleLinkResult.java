package com.prafta.web.acct.acct01.result;

/**
 * 근태 연계 — 정규직 당일 스케줄 결과 VO (tb_user_work_plan + tb_sch_mgmt 결합).
 * WORK_PLAN_CD 가 SCH_CD 인 경우에만 시각이 채워진다(LEAVE_CD 면 시각 null).
 */
public record ScheduleLinkResult(
    String workYmd
    , String workPlanCd
    , String schCd
    , String schType
    , String fstSchStrTime
    , String fstSchEndTime
    , String secSchStrTime
    , String secSchEndTime
){
}
