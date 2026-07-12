package com.prafta.web.chkLst.chkLst04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.dto.request.ChkptTargetListRequest;

public record ChkptTargetListParam(
	String siteCd
	, String chkLstType
	, String chkptNm
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ChkptTargetListParam from(ChkptTargetListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new ChkptTargetListParam(
			request.getSiteCd()
			, request.getChkLstType()
			, request.getChkptNm()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
