package com.prafta.web.attd.attd01.result;

/**
 * 특정 근무타입(SCH_CD)을 쓰는 근무계획(TB_USER_WORK_PLAN) 1건의 (사용자, 근무일).
 *
 * <p>prafta-com-016-A 공통 가드 ③: 근무타입의 시간/휴게 변경 시,
 * 그 타입을 쓰는 미래 적용분 근무계획 일자를 (userCd, workYmd) 로 모아
 * {@code ScheduleChangeGuardService.findLockedDays} 판정에 넘기기 위한 결과 홀더.
 */
public record WorkPlanDayResult(
	String userCd
	, String workYmd
){

}
