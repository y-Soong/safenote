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
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (stdTimeRuleType == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (stdTimeType == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AttdStdTimeRuleCommand(
    		stdTimeRuleType
    		, stdTimeType
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
