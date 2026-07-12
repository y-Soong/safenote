package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.SiteInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteInfoParam(
	String cmpnyCd
	, String userCd
	, String siteNo
	, String siteNm
	, String useYn   // 사용여부 필터('Y'/'N', 빈 값=전체). 일반(로그인 후) 조회 전용.
) {
	public static SiteInfoParam from(SiteInfoRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SiteInfoParam(
    		tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
    		, request.getSiteNo()
    		, request.getSiteNm()
    		, request.getUseYn()
        );
    }
}
