package com.prafta.web.acct.acct01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctCreateRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimItem;

public record AcctCreateParam(
    String siteCd
    , List<AcctVictimItem> victimList
    , String occurYmd
    , String occurTime
    , String occurPlace
    , String acctGradeCd
    , String acctDesc
    , String employerDesc
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctCreateParam from(AcctCreateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctCreateParam(
            request.getSiteCd()
            , request.getVictimList()
            , request.getOccurYmd()
            , request.getOccurTime()
            , request.getOccurPlace()
            , request.getAcctGradeCd()
            , request.getAcctDesc()
            , request.getEmployerDesc()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
