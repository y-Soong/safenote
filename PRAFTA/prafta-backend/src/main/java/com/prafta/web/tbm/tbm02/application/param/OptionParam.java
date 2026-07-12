package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.OptionRequest;

public record OptionParam(
	String siteCd
	, String searchKeyword
	, String processCd
	, String riskTypeCd
	, String hazardDesc
	, String initAssessorNm
	, String initAssessDate
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static OptionParam from(OptionRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new OptionParam(
			request.getSiteCd()
			, request.getSearchKeyword()
			, request.getProcessCd()
			, request.getRiskTypeCd()
			, request.getHazardDesc()
			, request.getInitAssessorNm()
			, request.getInitAssessDate()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
