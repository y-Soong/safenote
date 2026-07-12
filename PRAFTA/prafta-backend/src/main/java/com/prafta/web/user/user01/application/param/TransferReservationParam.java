package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.TransferReservationRequest;

/**
 * 소속이동 예약 등록 파라미터 — PRAFTA-WEB_001-1.
 *
 * <p>회사 스코프(gvCmpnyCd)/등록자(gvUserCd)/권한(gvAuthCd)은 JWT 클레임에서만 도출한다(클라 바디 신뢰 금지, IDOR/cross-tenant 방지).
 * 대상 userCd 와 이동 정보는 요청 바디값이며, 비즈니스 검증은 서비스/검증기에서 수행한다.
 */
public record TransferReservationParam(
    String userCd
    , String toSiteCd
    , String toNodeCd
    , String moveDate
    , String toDefaultSchCd
    , String moveReason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static TransferReservationParam from(TransferReservationRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TransferReservationParam(
            request.getUserCd()
            , request.getToSiteCd()
            , request.getToNodeCd()
            , request.getMoveDate()
            , request.getToDefaultSchCd()
            , request.getMoveReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
