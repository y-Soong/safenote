package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.CmpnyInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record CmpnyInfoParam(
	String cmpnyCd
) {
	public static CmpnyInfoParam from(CmpnyInfoRequest request) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new CmpnyInfoParam(
        		request.getCmpnyCd()
        );
    }
}
