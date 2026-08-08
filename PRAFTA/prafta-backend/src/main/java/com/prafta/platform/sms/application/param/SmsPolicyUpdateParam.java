package com.prafta.platform.sms.application.param;

import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.sms.dto.request.SmsPolicyUpdateRequest;

/**
 * SMS 발송 임계값 수정 파라미터(Platform_05).
 *
 * <p>★운영자 식별자({@code operatorUserCd})는 토큰에서만 도출한다. 요청 바디의 사용자 값을 신뢰하지 않는다
 *    ({@code PlatformCustomerController.updateTokenQuota} 선례 — IDOR 방지).
 *
 * <p>검증은 {@link #from} 에서 전부 수행한다(서비스 진입 시점에는 이미 유효한 값만 존재).
 */
public record SmsPolicyUpdateParam(
    int phoneWindowSec
    , int phoneHourLimit
    , int phoneDayLimit
    , String ipAxisEnabledYn
    , int ipHourLimit
    , int ipDayLimit
    , int userHourLimit
    , int userDayLimit
    , int globalHourLimit
    , String operatorUserCd
) {

    /**
     * 번호별 연속 발송 간격 상한(초).
     * ★59 를 넘길 수 없다. 프론트 재발송 타이머가 60초인데 창이 60 이상이면
     *   타이머 만료 직후 클릭이 초 절단 때문에 대부분 차단된다(1차 qa D-1 결함).
     *   화면을 통해 그 결함을 다시 들여오지 못하게 서버에서 막는다.
     */
    private static final int WINDOW_SEC_MAX = 59;

    /** 축별 상한 최대값(0 = 무제한). */
    private static final int AXIS_LIMIT_MAX = 10000;

    /**
     * 전역 상한 <b>하한</b>. [3차 / sec N-7]
     *
     * <p>★0(무제한)을 허용하지 않는다. {@code GLOBAL_HOUR_LIMIT=0} 이면
     * {@code SmsRateLimitGuard.checkGlobal} 이 즉시 return 하여 <b>킬스위치가 영구 무력화</b>된다.
     * 그 상태에서는 sysadmin 계정 하나만 탈취해도 API 호출 한 번으로 무제한 발송이 복원되고,
     * {@code PPURIO_ENABLED} 는 서버 secrets 소관이라 화면에서 되돌릴 수단이 없다.
     * 다른 축의 0(무제한)은 축 하나가 꺼질 뿐이지만 전역 축은 최후 방어선이라 성격이 다르다.
     */
    private static final int GLOBAL_LIMIT_MIN = 1;

    /** 전역 상한 최대값. */
    private static final int GLOBAL_LIMIT_MAX = 1000000;

    public static SmsPolicyUpdateParam from(SmsPolicyUpdateRequest request, String operatorUserCd) {

        if (request == null || operatorUserCd == null || operatorUserCd.isBlank()) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }

        int phoneWindowSec = nvl(request.getPhoneWindowSec());
        if (phoneWindowSec < 1 || phoneWindowSec > WINDOW_SEC_MAX) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }

        int phoneHourLimit = requireAxis(request.getPhoneHourLimit());
        int phoneDayLimit = requireAxis(request.getPhoneDayLimit());
        int ipHourLimit = requireAxis(request.getIpHourLimit());
        int ipDayLimit = requireAxis(request.getIpDayLimit());
        int userHourLimit = requireAxis(request.getUserHourLimit());
        int userDayLimit = requireAxis(request.getUserDayLimit());

        // ★[3차 / sec N-7] 하한 1 강제 — 0(무제한)이면 킬스위치가 영구 무력화된다.
        int globalHourLimit = nvl(request.getGlobalHourLimit());
        if (globalHourLimit < GLOBAL_LIMIT_MIN || globalHourLimit > GLOBAL_LIMIT_MAX) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }

        // 교차 검증: 시간당 상한이 일별 상한보다 크면 일별 상한이 의미를 잃는다(설정 실수 방지).
        // ★0(무제한)은 비교에서 제외한다 — "시간당 무제한 + 일별 20" 같은 조합은 유효하다.
        requireHourNotOverDay(phoneHourLimit, phoneDayLimit);
        requireHourNotOverDay(ipHourLimit, ipDayLimit);
        requireHourNotOverDay(userHourLimit, userDayLimit);

        String ipAxisEnabledYn = "Y".equals(request.getIpAxisEnabledYn()) ? "Y" : "N";

        return new SmsPolicyUpdateParam(
            phoneWindowSec
            , phoneHourLimit
            , phoneDayLimit
            , ipAxisEnabledYn
            , ipHourLimit
            , ipDayLimit
            , userHourLimit
            , userDayLimit
            , globalHourLimit
            , operatorUserCd
        );
    }

    private static int nvl(Integer v) {
        return v == null ? -1 : v;
    }

    private static int requireAxis(Integer v) {
        int value = nvl(v);
        if (value < 0 || value > AXIS_LIMIT_MAX) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }
        return value;
    }

    private static void requireHourNotOverDay(int hourLimit, int dayLimit) {
        if (hourLimit > 0 && dayLimit > 0 && hourLimit > dayLimit) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }
    }
}
