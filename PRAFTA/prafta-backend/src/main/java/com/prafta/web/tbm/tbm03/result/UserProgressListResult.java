package com.prafta.web.tbm.tbm03.result;

/**
 * T7 사용자별 TBM 진행 집계 행(1행=1사용자, GROUP BY USER_TYPE_CD/USER_CD).
 *
 * <p>집계 모수 = 입실 출결(ENTRY_AT NOT NULL). 누적 교육시간/최근 이수일은 이수(COMPLETED) 기준.
 * 일용직(DAILY)은 NODE_CD 가 없어 deptNm 은 NULL → FE '-' 표시.
 */
public record UserProgressListResult(
	String userTypeCd			// REGULAR / DAILY (SYS050)
	, String userTypeNm
	, String userCd
	, String userId				// 사번/아이디
	, String userNm				// 평문(복호화 불요)
	, String employmentTypeCd	// 정규직 EMPLOYMENT_TYPE(SYS041), 일용직 'DAILY'
	, String employmentTypeNm	// 정규직 SYS041 라벨, 일용직 '일용직'
	, String deptNm				// 정규직 소속 부서명(일용직 NULL)
	, int totalEduMinutes		// 누적 교육시간(이수 세션 EDU_MINUTES 합, D-4)
	, int completedSessionCount	// 수료 세션수
	, int notCompletedSessionCount	// 미이수 세션수(입실했으나 미완료)
	, String lastCompletedAt	// 최근 이수일(COMPLETED 의 STATUS_UPDATED_AT MAX, 없으면 NULL, D-5)
){
}
