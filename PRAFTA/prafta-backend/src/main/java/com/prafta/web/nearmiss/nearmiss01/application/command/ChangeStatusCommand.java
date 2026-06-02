package com.prafta.web.nearmiss.nearmiss01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.ChangeStatusParam;

public record ChangeStatusCommand(
    String siteCd
    , String nearMissId
    , String reportStatusCd
    , String rejectReason
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ChangeStatusCommand from(ChangeStatusParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChangeStatusCommand(
            param.siteCd()
            , param.nearMissId()
            , param.reportStatusCd()
            , param.rejectReason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
