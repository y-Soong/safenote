package com.prafta.app.entryadmin.entryadmin01.application.param;

import com.prafta.common.dto.TokenInfo;

/**
 * 앱 관리자 입장 승인 대기 목록 조회 파라미터.
 * 식별자(cmpny/site/user/auth)는 JWT 클레임에서만 도출한다(클라 파라미터 신뢰 금지).
 */
public record EntryPendingListParam(
    String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static EntryPendingListParam from(TokenInfo token) {
        return new EntryPendingListParam(
            token.gv_cmpnyCd()
            , token.gv_siteCd()
            , token.gv_userCd()
            , token.gv_authCd());
    }
}
