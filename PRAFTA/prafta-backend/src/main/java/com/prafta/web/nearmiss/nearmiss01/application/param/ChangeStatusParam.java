package com.prafta.web.nearmiss.nearmiss01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.dto.request.ChangeStatusRequest;

public record ChangeStatusParam(
    String siteCd
    , String nearMissId
    , String reportStatusCd
    , String rejectReason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ChangeStatusParam from(ChangeStatusRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ChangeStatusParam(
            request.getSiteCd()
            , request.getNearMissId()
            , request.getReportStatusCd()
            , request.getRejectReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
