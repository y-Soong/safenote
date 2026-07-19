package com.prafta.app.entryadmin.entryadmin01.application.param;

import java.util.List;

import com.prafta.app.entryadmin.entryadmin01.dto.request.EntryApproveRequest;
import com.prafta.common.dto.TokenInfo;

/**
 * 앱 관리자 입장 승인 처리 파라미터 (일괄 D9).
 * 처리자 식별은 JWT 클레임에서만 도출한다.
 */
public record EntryApproveParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , List<String> reqIds
) {
    public static EntryApproveParam of(EntryApproveRequest request, TokenInfo token) {
        return new EntryApproveParam(
            token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_authCd()
            , request == null ? null : request.reqIds());
    }
}
