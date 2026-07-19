package com.prafta.app.entryadmin.entryadmin01.application.param;

import com.prafta.app.entryadmin.entryadmin01.dto.request.EntryRejectRequest;
import com.prafta.common.dto.TokenInfo;

/**
 * 앱 관리자 입장 거부 처리 파라미터 (D10 — 사유 필수).
 * 처리자 식별은 JWT 클레임에서만 도출한다.
 */
public record EntryRejectParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String reqId
    , String reason
) {
    public static EntryRejectParam of(EntryRejectRequest request, TokenInfo token) {
        return new EntryRejectParam(
            token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_authCd()
            , request == null ? null : request.reqId()
            , request == null ? null : request.reason());
    }
}
