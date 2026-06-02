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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new WithdrawalCancelCommand(
			param.cmpnyCd()
			, param.userCd()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
