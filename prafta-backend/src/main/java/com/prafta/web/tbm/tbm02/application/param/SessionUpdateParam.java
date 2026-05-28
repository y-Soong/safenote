package com.prafta.web.tbm.tbm02.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;
import com.prafta.web.tbm.tbm02.dto.request.SessionUpdateRequest;

public record SessionUpdateParam(
	String sessionCd
	, String title
	, String contentBody
	, String gpsVerifyTypeCd
	, String managerGpsLat
	, String managerGpsLon
	, Integer gpsVerifyRadiusM
	, String gpsManualConfirmYn
	, List<SessionContentModel> contents
	, List<SessionRiskModel> risks
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static SessionUpdateParam from(SessionUpdateRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new SessionUpdateParam(
			request.getSessionCd()
			, request.getTitle()
			, request.getContentBody()
			, request.getGpsVerifyTypeCd()
			, request.getManagerGpsLat()
			, request.getManagerGpsLon()
			, request.getGpsVerifyRadiusM()
			, request.getGpsManualConfirmYn()
			, request.getContents() != null ? request.getContents() : Collections.emptyList()
			, request.getRisks() != null ? request.getRisks() : Collections.emptyList()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
