package com.prafta.web.attd.attd04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd04.dto.request.AttdStdTimeRuleListRequest;

public record AttdStdTimeRuleListParam(
    String gvCmpnyCd
) {
    public static AttdStdTimeRuleListParam from(TokenInfo tokenInfo) {


        return new AttdStdTimeRuleListParam(
            tokenInfo.gv_cmpnyCd()
        );
    }
}
