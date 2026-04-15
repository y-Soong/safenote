package com.prafta.web.attd.attd04.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleParam;

public record AttdStdTimeRuleCommand(
		String stdTimeRuleType
		, String stdTimeType
	    , String gvCmpnyCd
	    , String gvUserCd
) {
    public static AttdStdTimeRuleCommand from(AttdStdTimeRuleParam param, String stdTimeRuleType, String stdTimeType) {

        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - AttdStdTimeRuleParam");
        if (stdTimeRuleType == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - stdTimeRuleType");
        if (stdTimeType == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - stdTimeType");

        return new AttdStdTimeRuleCommand(
    		stdTimeRuleType
    		, stdTimeType
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
