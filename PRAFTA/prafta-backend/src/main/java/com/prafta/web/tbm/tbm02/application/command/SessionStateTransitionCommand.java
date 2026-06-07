package com.prafta.web.tbm.tbm02.application.command;

/**
 * 단순 상태 전이 UPDATE 커맨드(prafta-051-04/05).
 *
 * <p>교육시작(IN_PROGRESS)/교육준비 연장/교육종료(COMPLETED) 전이에 공용. 전이 대상
 * 상태/시각 조건은 각 매퍼 WHERE 절에서 경합 가드로 강제한다. 종료 전이는 종료비번을
 * 함께 발급하므로 exitPwd 를 채워 사용한다(그 외 전이는 null).
 */
public record SessionStateTransitionCommand(
	String sessionCd
	, String exitPwd			// 교육종료 전이 시에만 값(종료비번), 그 외 null
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionStateTransitionCommand of(
			String sessionCd, String gvCmpnyCd, String gvUserCd) {

		return new SessionStateTransitionCommand(sessionCd, null, gvCmpnyCd, gvUserCd);
	}

	public static SessionStateTransitionCommand ofComplete(
			String sessionCd, String exitPwd, String gvCmpnyCd, String gvUserCd) {

		return new SessionStateTransitionCommand(sessionCd, exitPwd, gvCmpnyCd, gvUserCd);
	}
}
