package com.prafta.web.chkLst.chkLst04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.param.ChkptTargetListParam;

public record ChkptTargetListQuery(
	String siteCd
	, String chkLstType
	, String chkptNm
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ChkptTargetListQuery from(ChkptTargetListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new ChkptTargetListQuery(
			param.siteCd()
			, param.chkLstType()
			, param.chkptNm()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
