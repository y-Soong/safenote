package com.prafta.web.chkLst.chkLst04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.dto.request.DefectListRequest;

public record DefectListParam(
	String siteCd
	, String chkLstType
	, String chkptCd
	, String inspectItemCd
	, String actionStatus
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DefectListParam from(DefectListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new DefectListParam(
			request.getSiteCd()
			, request.getChkLstType()
			, request.getChkptCd()
			, request.getInspectItemCd()
			, request.getActionStatus()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
