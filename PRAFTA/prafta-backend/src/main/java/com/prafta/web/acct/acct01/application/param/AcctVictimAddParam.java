package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctVictimAddRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimItem;

public record AcctVictimAddParam(
    String siteCd
    , String acctId
    , AcctVictimItem victim
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctVictimAddParam from(AcctVictimAddRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctVictimAddParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getVictim()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
