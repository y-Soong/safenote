package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;

/** TB_TBM_SESSION_RISK INSERT 커맨드(세션-위험성평가 매핑). */
public record SessionRiskCommand(
	String sessionCd
	, String siteCd
	, String processCd
	, String assessmentCd
	, int displayOrder
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionRiskCommand from(
			SessionRiskModel model, String sessionCd, int displayOrder,
			String gvCmpnyCd, String gvUserCd) {

		return new SessionRiskCommand(
			sessionCd
			, model.getSiteCd()
			, model.getProcessCd()
			, model.getAssessmentCd()
			, model.getDisplayOrder() != null ? model.getDisplayOrder() : displayOrder
			, gvCmpnyCd
			, gvUserCd
		);
	}
}
