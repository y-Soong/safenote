package com.prafta.web.chkLst.chkLst04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.dto.request.InspectItemListRequest;

public record InspectItemListParam(
	String chkLstType
	, String inspectItemSubj
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static InspectItemListParam from(InspectItemListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new InspectItemListParam(
			request.getChkLstType()
			, request.getInspectItemSubj()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
