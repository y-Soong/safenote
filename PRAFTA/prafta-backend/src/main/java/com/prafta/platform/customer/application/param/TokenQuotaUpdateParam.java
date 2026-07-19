package com.prafta.platform.customer.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.customer.dto.request.TokenQuotaUpdateRequest;

/**
 * AI 토큰 한도 변경 파라미터.
 *
 * <p>운영자 식별자(operatorUserCd)는 토큰에서만 도출한다(클라 입력 신뢰 금지 — PlatformOperatorParam 전례).
 * 모드/값 검증(PLATFORM_400_014)과 원시 토큰 변환은 여기서 확정하고, 대상 회사 존재 검증(PLATFORM_400_015)은
 * 서비스가 DB 로 수행한다.
 *
 * <p>cmpnyCd 는 운영자 전용 콘솔이므로 전 고객사 대상 허용이 설계 의도(IDOR 아님 — 게이트가 운영자+IP 강제).
 */
public record TokenQuotaUpdateParam(
    String cmpnyCd
    , long monthlyTokenLimit
    , String operatorUserCd
) {
    /** 한도 설정 모드. */
    private static final String MODE_LIMIT = "LIMIT";
    /** 무제한 모드(-1 저장). */
    private static final String MODE_UNLIMITED = "UNLIMITED";
    /** 완전 차단 모드(0 저장). */
    private static final String MODE_BLOCK = "BLOCK";

    /** LIMIT 모드 만 단위 하한. */
    private static final int LIMIT_MAN_MIN = 1;
    /** LIMIT 모드 만 단위 상한(= 100억 토큰 sanity 상한 — 요청서 §4). */
    private static final int LIMIT_MAN_MAX = 1_000_000;

    /** 만 → 원시 토큰 환산 계수. */
    private static final long TOKENS_PER_MAN = 10_000L;

    public static TokenQuotaUpdateParam from(TokenQuotaUpdateRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null || request.getCmpnyCd() == null || request.getCmpnyCd().trim().isEmpty()) {
            // 대상 회사 미지정 → 미존재와 동일 취급.
            throw new ApiException(PlatformErrorCode.PLATFORM_400_015);
        }

        return new TokenQuotaUpdateParam(
            request.getCmpnyCd().trim()
            , resolveLimit(request.getQuotaMode(), request.getLimitMan())
            , tokenInfo.gv_userCd()
        );
    }

    /** 모드/값 → 저장용 원시 토큰 한도. 위반 시 PLATFORM_400_014. */
    private static long resolveLimit(String quotaMode, Integer limitMan) {
        if (MODE_UNLIMITED.equals(quotaMode)) {
            return -1L;
        }
        if (MODE_BLOCK.equals(quotaMode)) {
            return 0L;
        }
        if (MODE_LIMIT.equals(quotaMode)) {
            if (limitMan == null || limitMan < LIMIT_MAN_MIN || limitMan > LIMIT_MAN_MAX) {
                throw new ApiException(PlatformErrorCode.PLATFORM_400_014);
            }
            return limitMan * TOKENS_PER_MAN;
        }
        // 미상 모드.
        throw new ApiException(PlatformErrorCode.PLATFORM_400_014);
    }
}
