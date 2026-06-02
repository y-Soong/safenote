package com.prafta.web.tbm.tbm04.application.command;

import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;

/**
 * TB_TBM_ATTENDANCE 이수상태 사후 변경(W-14) UPDATE 커맨드.
 *
 * <p>COMPLETION_STATUS_CD / NOT_COMPLETED_REASON / STATUS_UPDATED_BY / STATUS_UPDATED_AT 갱신.
 * COMPLETED 복귀 시에도 NOT_COMPLETED_REASON 은 보존(감사용) — mapper 에서 NOT_COMPLETED 일 때만 사유를 덮어쓴다.
 */
public record CompletionUpdateCommand(
	String attendanceCd
	, String completionStatusCd
	, String notCompletedReason	// NOT_COMPLETED 시에만 세팅(COMPLETED 시 null → 사유 보존)
	, String gvCmpnyCd
	, String gvUserCd
){
	public static CompletionUpdateCommand from(CompletionUpdateParam param) {

		boolean notCompleted = "NOT_COMPLETED".equals(param.completionStatusCd());
		String reason = notCompleted
				? (param.reason() != null ? param.reason().trim() : null)
				: null;

		return new CompletionUpdateCommand(
			param.attendanceCd()
			, param.completionStatusCd()
			, reason
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
