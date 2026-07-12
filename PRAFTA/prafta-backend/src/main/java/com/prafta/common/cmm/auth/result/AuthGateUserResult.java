package com.prafta.common.cmm.auth.result;

/**
 * prafta-app-033: 로그인 게이트(강제 비밀번호 변경) 판정용 사용자 1행 조회 결과.
 *
 * <ul>
 *   <li>pwdChgPendingYn : PWD_CHG_DTIME IS NULL 이면 'Y'(비번 변경 미완료), 아니면 'N'.</li>
 *   <li>employmentType  : 고용형태(SYS041). 'DAILY' 면 비번 게이트 제외(데드락 방지).</li>
 * </ul>
 */
public record AuthGateUserResult(
	String pwdChgPendingYn
	, String employmentType
) {
}
