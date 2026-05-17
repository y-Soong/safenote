package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;

public record DailyAttdDetailDeleteCommand(
    String siteCd
    , String userCd
    , String attdId
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static DailyAttdDetailDeleteCommand from(DailyAttdDetailDeleteParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DailyAttdDetailDeleteCommand(
            param.siteCd()
            , param.userCd()
            , param.attdId()
            , param.reason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
