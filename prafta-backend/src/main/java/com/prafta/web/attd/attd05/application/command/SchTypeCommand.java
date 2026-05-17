package com.prafta.web.attd.attd05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;

public record SchTypeCommand(
	String siteCd
	, String userCd
	, String workYmd
	, String workPlanCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	
	public static SchTypeCommand from(SchTypeModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchTypeCommand(
    		model.siteCd()
    		, model.userCd()
    		, model.workYmd()
    		, model.workPlanCd()
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
		);
    }
}
