package com.prafta.web.user.user02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user02.application.model.AuthMenuInfoModel;

public record AuthMenuInfoCommand(
	String authCd
	, String menuDId
	, String useYn
	, String btnSrch
	, String btnNew
	, String btnDel
	, String btnSave
	, String btnExcl
	
	, String gvCmpnyCd
	, String gvUserCd
){
	public static AuthMenuInfoCommand from(AuthMenuInfoModel model) {
		
		if(model == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - AuthMenuInfoModel");
		
		return new AuthMenuInfoCommand(
			model.authCd()
			, model.menuDId()
			, model.useYn()
			, model.btnSrch()
			, model.btnNew()
			, model.btnDel()
			, model.btnSave()
			, model.btnExcl()
			, model.gvCmpnyCd()
			, model.gvUserCd()
		); 
	}
}
