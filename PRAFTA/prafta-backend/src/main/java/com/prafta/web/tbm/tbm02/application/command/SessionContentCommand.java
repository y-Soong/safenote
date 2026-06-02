package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;

/** TB_TBM_SESSION_CONTENT INSERT 커맨드(세션-콘텐츠 묶음 매핑). */
public record SessionContentCommand(
	String sessionCd
	, String mtrlCd
	, int displayOrder
	, String overrideDesc
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionContentCommand from(
			SessionContentModel model, String sessionCd, int displayOrder,
			String gvCmpnyCd, String gvUserCd) {

		return new SessionContentCommand(
			sessionCd
			, model.getMtrlCd()
			, model.getDisplayOrder() != null ? model.getDisplayOrder() : displayOrder
			, model.getOverrideDesc()
			, gvCmpnyCd
			, gvUserCd
		);
	}
}
