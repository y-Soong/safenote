package com.prafta.web.attd.attd04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd04.dto.request.AttdStdTimeRuleListRequest;

public record AttdStdTimeRuleListParam(
    String gvCmpnyCd
) {
    public static AttdStdTimeRuleListParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TokenInfo");

        return new AttdStdTimeRuleListParam(
            tokenInfo.gv_cmpnyCd()
        );
    }
}
