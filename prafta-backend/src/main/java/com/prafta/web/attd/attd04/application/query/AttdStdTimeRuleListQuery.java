package com.prafta.web.attd.attd04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleListParam;

public record AttdStdTimeRuleListQuery(
    String gvCmpnyCd
) {
    public static AttdStdTimeRuleListQuery from(AttdStdTimeRuleListParam param) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수 값 누락 - Attd04ListParam");

        return new AttdStdTimeRuleListQuery(
            param.gvCmpnyCd()
        );
    }
}
