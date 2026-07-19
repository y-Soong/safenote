package com.prafta.web.chkLst.chkLst04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.dto.request.InspectItemListRequest;

public record InspectItemListParam(
	String siteCd
	, String chkLstType
	, String inspectItemSubj
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static InspectItemListParam from(InspectItemListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// PRAFTA-SUBCON-T0-03: 문항 사업장 분리 — siteCd 미전달 시 400
		if (request.getSiteCd() == null || request.getSiteCd().isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001, "사업장코드는 필수입니다.");

		return new InspectItemListParam(
			request.getSiteCd()
			, request.getChkLstType()
			, request.getInspectItemSubj()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
