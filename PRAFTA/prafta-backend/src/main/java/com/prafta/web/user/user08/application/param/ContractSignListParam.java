package com.prafta.web.user.user08.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user08.dto.request.ContractSignListRequest;

/**
 * 서명 이력 목록 조회 파라미터 (웹 User_08 탭2).
 * 회사/처리자 식별은 JWT 클레임에서만 도출한다.
 */
public record ContractSignListParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String siteCd
    , String fromDate
    , String toDate
    , String userNm
) {
    public static ContractSignListParam from(ContractSignListRequest request, TokenInfo token) {
        return new ContractSignListParam(
            token.gv_cmpnyCd()
            , token.gv_userCd()
            , token.gv_authCd()
            , request.getSiteCd()
            , request.getFromDate()
            , request.getToDate()
            , request.getUserNm());
    }
}
