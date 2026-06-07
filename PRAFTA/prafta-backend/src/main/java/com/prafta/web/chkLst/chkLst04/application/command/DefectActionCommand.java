package com.prafta.web.chkLst.chkLst04.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;

public record DefectActionCommand(
	String siteCd
	, String chkptCd
	, String inspectItemCd
	, String workDate
	, String actionDesc
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DefectActionCommand from(DefectActionParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new DefectActionCommand(
			param.siteCd()
			, param.chkptCd()
			, param.inspectItemCd()
			, param.workDate()
			, param.actionDesc()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
