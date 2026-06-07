package com.prafta.web.chkLst.chkLst04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.param.InspectItemListParam;

public record InspectItemListQuery(
	String chkLstType
	, String inspectItemSubj
	, String gvCmpnyCd
	, String gvUserCd
){
	public static InspectItemListQuery from(InspectItemListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new InspectItemListQuery(
			param.chkLstType()
			, param.inspectItemSubj()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
