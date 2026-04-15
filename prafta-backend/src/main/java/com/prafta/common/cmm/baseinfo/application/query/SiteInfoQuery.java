package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.SiteInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteInfoQuery(
		String cmpnyCd
		, String userCd
		, String siteNo
		, String siteNm
) {
	public static SiteInfoQuery from(SiteInfoParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteInfoParam");
		
        return new SiteInfoQuery(
    		param.cmpnyCd()
    		, param.userCd()
    		, param.siteNo()
    		, param.siteNm()
        );
    }
}
