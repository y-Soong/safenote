package com.prafta.web.nearmiss.nearmiss01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.dto.request.ReclassifyRequest;

public record ReclassifyParam(
    String siteCd
    , String srcProcessCd
    , String srcAssessmentCd
    , String incidentTypeCd
    , String processCd
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String immediateActionDesc
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ReclassifyParam from(ReclassifyRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ReclassifyParam(
            request.getSiteCd()
            , request.getSrcProcessCd()
            , request.getSrcAssessmentCd()
            , request.getIncidentTypeCd()
            , request.getProcessCd()
            , request.getOccurDtime()
            , request.getLocationDesc()
            , request.getDescription()
            , request.getPotentialSeverityCd()
            , request.getImmediateActionDesc()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
