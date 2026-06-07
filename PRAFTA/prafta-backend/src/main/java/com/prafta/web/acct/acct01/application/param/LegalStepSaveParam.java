package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.LegalStepSaveRequest;

public record LegalStepSaveParam(
    String siteCd
    , String acctId
    , String stepCd
    , String isDoneYn
    , String remark
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static LegalStepSaveParam from(LegalStepSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LegalStepSaveParam(
            request.getSiteCd()
            , request.getAcctId()
            , request.getStepCd()
            , request.getIsDoneYn()
            , request.getRemark()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
