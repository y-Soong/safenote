package com.prafta.platform.location.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 플랫폼 운영자 식별 파라미터(sms-send / sms-status 공용).
 *
 * <p>운영자 식별자는 토큰에서만 도출한다(클라 입력 신뢰 금지 — 발송 대상 위조 불가).
 * 게이트(PlatformOperatorGateInterceptor)가 gv_cmpnyCd = prafta_system_admin 을 이미 강제한다.
 */
public record PlatformOperatorParam(
    String gvCmpnyCd
    , String gvUserCd
) {
    public static PlatformOperatorParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new PlatformOperatorParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }
}
