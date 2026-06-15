package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-002: 사용자 근무계획 + 스케줄 정의 조인 결과.
 *
 * <p>매핑 대상: AppAttd01Mapper.selectScheduleByRange (TB_USER_WORK_PLAN + TB_SCH_MGMT LEFT JOIN).
 * <p>prafta-com-008-E-2: 연차-스케줄 모델 전환 — WORK_PLAN_CD 는 항상 SCH_CD 만 가리킨다.
 *   연차일 판정은 work_plan 이 아니라 TB_USER_LEAVE_USE(CONFIRMED, 종일) 기준으로 단일화한다.
 *   leaveCd/leaveNm 은 더 이상 work_plan 에서 채워지지 않으며 항상 null(record 위치매핑 유지를 위해 필드만 잔존).
 * <p>시간 컬럼은 모두 varchar(4) HHMM, 휴게분(FST/SEC_SCH_BRK_MIN)은 varchar(3).
 *   2구간 여부는 secSchStrTime 이 not null 인지로 판정한다.
 */
public record ScheduleResult(
    String workYmd
    , String workPlanCd
    , String schCd
    , String schNo
    , String schType
    , String fstSchStrTime
    , String fstSchEndTime
    , String fstSchBrkMin
    , String secSchStrTime
    , String secSchEndTime
    , String secSchBrkMin
    , String leaveCd
    , String leaveNm
) {
}
