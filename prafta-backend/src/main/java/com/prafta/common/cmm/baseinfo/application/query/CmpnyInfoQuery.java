package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.CmpnyInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record CmpnyInfoQuery(
	String cmpnyCd
) {
	public static CmpnyInfoQuery from(CmpnyInfoParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new CmpnyInfoQuery(
        		param.cmpnyCd()
        );
    }
}
