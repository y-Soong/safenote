package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminSessionCancelRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/** T-A4 관리자 TBM 세션 취소 파라미터. sessionCd 는 path 에서 받는다. */
public record AdminSessionCancelParam(
    String sessionCd
    , String cancelReason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminSessionCancelParam from(String sessionCd, AdminSessionCancelRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminSessionCancelParam(
            sessionCd
            , request.getCancelReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
