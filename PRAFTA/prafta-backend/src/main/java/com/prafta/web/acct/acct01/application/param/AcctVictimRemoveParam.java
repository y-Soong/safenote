package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctVictimRemoveRequest;

public record AcctVictimRemoveParam(
    String siteCd
    , String acctId
    , Integer victimSeq
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctVictimRemoveParam from(AcctVictimRemoveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctVictimRemoveParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getVictimSeq()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
