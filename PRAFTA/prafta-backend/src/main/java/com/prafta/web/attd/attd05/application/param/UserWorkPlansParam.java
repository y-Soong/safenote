package com.prafta.web.attd.attd05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.dto.request.UserWorkPlansRequest;


public record UserWorkPlansParam (
	String siteCd
	, String nodeCd
	, String incSubNodeYn
	, String workYm
	, String userNm
	, String gvCmpnyCd
	// com-013-04-FU-r19: 그리드 조회 인가 가드용 — 토큰 권위 식별값(본문 비신뢰). 저장용 SchTypeParam 과 대칭.
	, String gvAuthCd
	, String gvUserCd
){
	public static UserWorkPlansParam from(UserWorkPlansRequest request, TokenInfo tokenInfo) {

        if (request == null || tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserWorkPlansParam(
    		request.getSiteCd()
    		, request.getNodeCd()
    		, request.getIncSubNodeYn()
    		, request.getWorkYm()
    		, request.getUserNm()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_authCd()
    		, tokenInfo.gv_userCd()
		);
    }
}
