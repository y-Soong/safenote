package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.ScheduleWithdrawalRequest;

public record ScheduleWithdrawalParam(
    String cmpnyCd
    , String userCd
    , String withdrawalDate		/** YYYYMMDD */
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static ScheduleWithdrawalParam from(ScheduleWithdrawalRequest request, TokenInfo tokenInfo) {
        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nrequired param - ScheduleWithdrawalRequest");

        // Convert YYYY-MM-DD -> YYYYMMDD
        String date = request.getWithdrawalDate();
        if (date != null) {
            date = date.replace("-", "");
        }

        return new ScheduleWithdrawalParam(
            request.getCmpnyCd()
            , request.getUserCd()
            , date
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
