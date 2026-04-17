package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;

public record ScheduleWithdrawalCommand(
    String cmpnyCd
    , String userCd
    , String withdrawalDate
    , String gvCmpnyCd
    , String gvUserCd
) {
	public static ScheduleWithdrawalCommand from(ScheduleWithdrawalParam param) {
        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nrequired param - ScheduleWithdrawalParam");

        return new ScheduleWithdrawalCommand(
    		param.cmpnyCd()
            , param.userCd()
            , param.withdrawalDate()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
