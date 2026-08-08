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
    // SMS2-B4: 요청 IP 해시(IP 축 상한 재료). sms-send 에서만 채워지고 sms-status 는 null 이다.
    // ★확정하지 못하면 null 이며 그때는 IP 축을 판정하지 않는다(fail-open).
    , String ipHash
) {
    /** sms-status 등 IP 축이 필요 없는 경로용(ipHash = null). */
    public static PlatformOperatorParam from(TokenInfo tokenInfo) {
        return from(tokenInfo, null);
    }

    public static PlatformOperatorParam from(TokenInfo tokenInfo, String ipHash) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new PlatformOperatorParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , ipHash
        );
    }
}
