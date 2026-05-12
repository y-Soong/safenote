package com.prafta.web.attd.attd05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.SchTypeDeleModel;

public record SchTypeDeleCommand(
	String siteCd
	, String userCd
	, String workYm
	, String gvCmpnyCd
	, String gvUserCd
) {
	
	public static SchTypeDeleCommand from(SchTypeDeleModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchTypeDeleModel");

        return new SchTypeDeleCommand(
    		model.siteCd()
    		, model.userCd()
    		, model.workYm()
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
		);
    }
}
