package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctVictimListRequest;

public record AcctVictimListParam(
    String siteCd
    , String acctId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctVictimListParam from(AcctVictimListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctVictimListParam(
            request.getSiteCd()
            , request.getAcctId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
