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
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - WithdrawMyAccountParam");
        }

        return new WithdrawMyAccountCommand(
    		param.cmpnyCd()
    		, param.userCd()
		);
    }
}