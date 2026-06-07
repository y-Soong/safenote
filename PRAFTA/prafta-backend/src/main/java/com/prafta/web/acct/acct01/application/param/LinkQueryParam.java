package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.LinkQueryRequest;

public record LinkQueryParam(
    String siteCd
    , String acctId
    , String chklstType
    , String chkptCd
    , String processCd
    , String riskTypeCd
    , String hazardCd
    , String incidentTypeCd
    , String potentialSeverityCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LinkQueryParam from(LinkQueryRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LinkQueryParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getChklstType()
            , request.getChkptCd()
            , request.getProcessCd()
            , request.getRiskTypeCd()
            , request.getHazardCd()
            , request.getIncidentTypeCd()
            , request.getPotentialSeverityCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
