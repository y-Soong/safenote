package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.AcctVictimUpdateRequest;

/**
 * 재해자 속성 수정 파라미터. mapper updateAcctVictimAttr 의 parameterType 으로 직접 바인딩.
 */
public record AcctVictimUpdateParam(
    String siteCd
    , String acctId
    , Integer victimSeq
    , String victimResultCd
    , Integer careDays
    , Integer restDays
    , String injuryPart
    , String injuryDesc
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AcctVictimUpdateParam from(AcctVictimUpdateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AcctVictimUpdateParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getVictimSeq()
            , request.getVictimResultCd()
            , request.getCareDays()
            , request.getRestDays()
            , request.getInjuryPart()
            , request.getInjuryDesc()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
