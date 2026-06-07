package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R3 T1 교육 시작/종료(상태 전이) 파라미터.
 *
 * <p>sessionCd 는 path 에서 받되 식별자(회사/사용자/사업장/권한)는 JWT 클레임에서만 도출한다(IDOR 차단).
 */
public record AdminLiveTransitionParam(
    String sessionCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminLiveTransitionParam of(String sessionCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminLiveTransitionParam(
            sessionCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
