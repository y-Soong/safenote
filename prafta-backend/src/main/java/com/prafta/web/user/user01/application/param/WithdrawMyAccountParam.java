package com.prafta.web.user.user01.application.param;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.WithdrawMyAccountRequest;

public record WithdrawMyAccountParam(
    String cmpnyCd,
    String userCd
) {
    public static WithdrawMyAccountParam from(WithdrawMyAccountRequest request) {
        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nrequired param - WithdrawMyAccountRequest");
        return new WithdrawMyAccountParam(
            request.getCmpnyCd(),
            request.getUserCd()
        );
    }
}
