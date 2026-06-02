package com.prafta.web.nearmiss.nearmiss01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.dto.request.IncidentListRequest;

public record IncidentListParam(
    String siteCd
    , String reportStatusCd
    , String incidentTypeCd
    , String potentialSeverityCd
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static IncidentListParam from(IncidentListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new IncidentListParam(
            request.getSiteCd()
            , request.getReportStatusCd()
            , request.getIncidentTypeCd()
            , request.getPotentialSeverityCd()
            , request.getStartDate()
            , request.getEndDate()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
