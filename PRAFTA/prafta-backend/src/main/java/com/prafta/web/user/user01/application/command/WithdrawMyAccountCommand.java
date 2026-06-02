package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;

public record WithdrawMyAccountCommand(
    String cmpnyCd,
    String userCd
) {
	public static WithdrawMyAccountCommand from(WithdrawMyAccountParam param) {

        if (param == null) {
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new WithdrawMyAccountCommand(
    		param.cmpnyCd()
    		, param.userCd()
		);
    }
}