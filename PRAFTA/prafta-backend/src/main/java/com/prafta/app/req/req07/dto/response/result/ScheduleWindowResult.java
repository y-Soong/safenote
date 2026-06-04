package com.prafta.app.req.req07.dto.response.result;

/**
 * prafta-app-017: OT 겹침 검증용 — 해당 근무일의 근무계획 스케줄 1건(1·2구간 시각).
 *
 * <p>TB_USER_WORK_PLAN.WORK_PLAN_CD 가 SCH_CD 일 때만 TB_SCH_MGMT 조인이 성립한다.
 * WORK_PLAN_CD 가 연차코드(LEAVE_CD)거나 NULL/미존재면 결과 0행 → null 반환 → 정규구간 부재 →
 * 겹침검사 면제(전량 OT 허용).
 *
 * <p>시각 필드는 'HHmm' 4자리 문자열 (tb_sch_mgmt 의 varchar(4)). 2구간이 없는 스케줄은
 * secStrTime / secEndTime 이 null.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *    AppReq07Mapper.selectWorkPlanSchedule 의 SELECT 컬럼 순서를 아래 인자 순서와 100% 일치시킬 것.
 */
public record ScheduleWindowResult(
        String schCd
        , String fstStrTime   // HHmm
        , String fstEndTime   // HHmm
        , String secStrTime   // HHmm (nullable)
        , String secEndTime   // HHmm (nullable)
) {
}
