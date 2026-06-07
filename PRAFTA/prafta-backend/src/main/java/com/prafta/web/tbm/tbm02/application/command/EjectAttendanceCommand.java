package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.param.EjectAttendanceParam;

/**
 * 입실자 내보내기(soft delete) 커맨드(prafta-051-12).
 *
 * <p>DEL_YN='Y' + EXIT_FORCED_REASON 에 사유 기록(책임 보존). 세션/출결 일치 + DEL_YN='N'
 * 조건의 행만 갱신하며, 영향행 0 이면 이미 제거/부적합으로 거부한다.
 */
public record EjectAttendanceCommand(
	String gvCmpnyCd
	, String sessionCd
	, String attendanceCd
	, String reason
	, String gvUserCd
){
	public static EjectAttendanceCommand of(EjectAttendanceParam param) {
		return new EjectAttendanceCommand(
			param.gvCmpnyCd()
			, param.sessionCd()
			, param.attendanceCd()
			, param.reason()
			, param.gvUserCd()
		);
	}
}
