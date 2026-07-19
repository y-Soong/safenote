package com.prafta.web.user.user08.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user08.dto.request.EntryRequestListRequest;

/**
 * 입장 승인요청 목록 조회 파라미터 (웹 User_08 탭1).
 * 회사/처리자 식별은 JWT 클레임에서만 도출한다.
 */
public record EntryRequestListParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String siteCd
    , String reqStatus
    , String reqType
    , String reqDate
) {
    public static EntryRequestListParam from(EntryRequestListRequest request, TokenInfo token) {
        return new EntryRequestListParam(
            token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_authCd()
            , request.getSiteCd()
            , request.getReqStatus()
            , request.getReqType()
            , request.getReqDate());
    }
}
