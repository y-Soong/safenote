package com.prafta.web.nearmiss.nearmiss01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.dto.request.IncidentInfoRequest;

public record IncidentInfoParam(
    String siteCd
    , String nearMissId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static IncidentInfoParam from(IncidentInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new IncidentInfoParam(
            request.getSiteCd()
            , request.getNearMissId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
