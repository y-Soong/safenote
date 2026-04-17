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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - WithdrawalCancelRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
		return new WithdrawalCancelParam(
			request.getCmpnyCd()
			, request.getUserCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
