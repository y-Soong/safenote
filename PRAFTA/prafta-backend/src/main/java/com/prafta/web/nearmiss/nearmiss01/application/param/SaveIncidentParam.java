package com.prafta.web.nearmiss.nearmiss01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.dto.request.SaveIncidentRequest;

public record SaveIncidentParam(
    String siteCd
    , String nearMissId
    , String causeDesc
    , String preventionDesc
    , String immediateActionDesc
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SaveIncidentParam from(SaveIncidentRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SaveIncidentParam(
            request.getSiteCd()
            , request.getNearMissId()
            , request.getCauseDesc()
            , request.getPreventionDesc()
            , request.getImmediateActionDesc()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
