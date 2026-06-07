package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * T-A2 관리자 TBM 세션 상세 조회 파라미터.
 *
 * <p>sessionCd 는 path 에서 받되 식별자(회사/사용자/사업장/권한)는 JWT 클레임에서만 도출한다.
 */
public record AdminSessionDetailParam(
    String sessionCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminSessionDetailParam of(String sessionCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminSessionDetailParam(
            sessionCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
