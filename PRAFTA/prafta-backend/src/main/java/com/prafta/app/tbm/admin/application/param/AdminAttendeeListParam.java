package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R3 출결 리스트(LIVE/COMPLETED) 파라미터.
 *
 * <p>phase=LIVE(입실자만, ENTRY_AT IS NOT NULL) / COMPLETED(출결 전체). sessionCd 는 path 에서 받는다.
 * 식별자는 JWT 클레임에서만 도출(IDOR 차단).
 */
public record AdminAttendeeListParam(
    String sessionCd
    , String phase
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminAttendeeListParam of(String sessionCd, String phase, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminAttendeeListParam(
            sessionCd
            , phase
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
