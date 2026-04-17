package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;

public record WithdrawalCancelCommand (
	String cmpnyCd
    , String userCd
    , String gvCmpnyCd
    , String gvUserCd
){
	public static WithdrawalCancelCommand from(WithdrawalCancelParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - WithdrawalCancelParam");
		
		return new WithdrawalCancelCommand(
			param.cmpnyCd()
			, param.userCd()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
