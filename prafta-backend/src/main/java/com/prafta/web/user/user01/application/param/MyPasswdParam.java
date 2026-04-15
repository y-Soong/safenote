package com.prafta.web.user.user01.application.param;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.MyPasswdRequest;

public record MyPasswdParam(
    String cmpnyCd,
    String userCd,
    String currentPw,
    String newPw
) {
    public static MyPasswdParam from(MyPasswdRequest request) {
        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nrequired param - MyPasswdRequest");
        return new MyPasswdParam(
            request.getCmpnyCd(),
            request.getUserCd(),
            request.getCurrentPw(),
            request.getNewPw()
        );
    }
}
