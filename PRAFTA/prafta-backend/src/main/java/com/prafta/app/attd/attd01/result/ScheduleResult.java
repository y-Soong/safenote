package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-002: 사용자 근무계획 + 스케줄 정의 조인 결과.
 *
 * <p>매핑 대상: AppAttd01Mapper.selectScheduleByDate / selectScheduleByRange
 *   (TB_USER_WORK_PLAN + TB_SCH_MGMT + TB_LEAVE_TYPE_MGMT LEFT JOIN).
 * <p>WORK_PLAN_CD 는 SCH_CD(근무) 또는 LEAVE_CD(연차) 를 가리킨다.
 *   - 근무계획이 SCH_CD 인 경우: schXxx 컬럼이 채워지고 leaveNm 은 null.
 *   - 근무계획이 LEAVE_CD 인 경우: schXxx 는 null, leaveNm 이 채워진다.
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
