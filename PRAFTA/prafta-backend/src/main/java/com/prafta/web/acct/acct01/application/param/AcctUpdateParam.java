package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctUpdateRequest;

public record AcctUpdateParam(
    String siteCd
    , String acctId
    , String occurYmd
    , String occurTime
    , String occurPlace
    , String acctGradeCd
    , String acctDesc
    , String employerDesc
    , String processStatusCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctUpdateParam from(AcctUpdateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctUpdateParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getOccurYmd()
            , request.getOccurTime()
            , request.getOccurPlace()
            , request.getAcctGradeCd()
            , request.getAcctDesc()
            , request.getEmployerDesc()
            , request.getProcessStatusCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
