package com.prafta.web.acct.acct01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.dto.request.ChkptOptionRequest;

public record ChkptOptionParam(
    String siteCd
    , String chklstType
    , String chkptNm
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ChkptOptionParam from(ChkptOptionRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ChkptOptionParam(
            request.getSiteCd()
            , request.getChklstType()
            , request.getChkptNm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
