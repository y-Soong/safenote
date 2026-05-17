package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.WithdrawalCancelRequest;

public record WithdrawalCancelParam (
	String cmpnyCd
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
){
	public static WithdrawalCancelParam from(WithdrawalCancelRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
		return new WithdrawalCancelParam(
			request.getCmpnyCd()
			, request.getUserCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
