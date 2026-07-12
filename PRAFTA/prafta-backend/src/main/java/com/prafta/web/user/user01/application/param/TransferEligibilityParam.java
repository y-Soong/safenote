package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 소속이동 가능 여부 사전 판정 파라미터 — PRAFTA-WEB_001-1.
 *
 * <p>대상 userCd 는 path variable, 회사 스코프/권한은 토큰에서만 도출한다(IDOR/cross-tenant 방지).
 * toDefaultSchCd/toSiteCd/moveDate 는 선택 쿼리값(불가케이스 ⑤ 시간차 연차 커버리지 판정에 사용).
 */
public record TransferEligibilityParam(
    String userCd
    , String toSiteCd
    , String toDefaultSchCd
    , String moveDate
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static TransferEligibilityParam from(String userCd, String toSiteCd, String toDefaultSchCd,
            String moveDate, TokenInfo tokenInfo) {

        if (userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TransferEligibilityParam(
            userCd
            , toSiteCd
            , toDefaultSchCd
            , moveDate
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
