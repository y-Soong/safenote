package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;

/** TB_TBM_SESSION 취소(STATUS_CD='CANCELLED') UPDATE 커맨드. */
public record SessionCancelCommand(
	String sessionCd
	, String cancelReason
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionCancelCommand from(SessionCancelParam param) {

		return new SessionCancelCommand(
			param.sessionCd()
			, param.cancelReason() != null ? param.cancelReason().trim() : null
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
