package com.prafta.platform.customer.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.error.stdwork.StdWorkErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.customer.dto.request.StdWorkPolicyUpdateRequest;

/**
 * 회사 통상근로시간 기준값 변경 파라미터.
 *
 * <p>운영자 식별자(operatorUserCd)는 토큰에서만 도출한다(클라 입력 신뢰 금지 — TokenQuotaUpdateParam 전례).
 * 모드 해석만 여기서 확정하고, <b>값 범위 검증(0 초과 ~ 2400분)은 저장 단일 출처인
 * {@code StdWorkHoursService.saveWeekStdMinutesPolicy} 가 수행</b>한다 — 법정 상한 근거 문구
 * ({@code STDWORK_400_007})를 한 곳에서만 관리하기 위함이다.
 *
 * <p>대상 회사 존재 검증(PLATFORM_400_015)은 서비스가 DB 로 수행한다.
 *
 * <p>cmpnyCd 는 운영자 전용 콘솔이므로 전 고객사 대상 허용이 설계 의도(IDOR 아님 — 게이트가 운영자+IP 강제).
 */
public record StdWorkPolicyUpdateParam(
    String cmpnyCd
    // 주 소정근로 분. null = 지정 해제(행 삭제 → 코드 폴백 2400분).
    , Integer weekStdMinutes
    , String operatorUserCd
) {
    /** 직접 지정 모드. */
    private static final String MODE_DIRECT = "DIRECT";
    /** 지정 해제 모드(행 삭제 → 주 40시간 폴백). */
    private static final String MODE_DEFAULT = "DEFAULT";

    public static StdWorkPolicyUpdateParam from(StdWorkPolicyUpdateRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null || request.getCmpnyCd() == null || request.getCmpnyCd().trim().isEmpty()) {
            // 대상 회사 미지정 → 미존재와 동일 취급.
            throw new ApiException(PlatformErrorCode.PLATFORM_400_015);
        }

        return new StdWorkPolicyUpdateParam(
            request.getCmpnyCd().trim()
            , resolveMinutes(request.getPolicyMode(), request.getWeekStdMinutes())
            , tokenInfo.gv_userCd()
        );
    }

    /** 모드/값 → 저장용 분. 지정 해제는 null. 미상 모드/DIRECT 값 누락은 필수값 오류. */
    private static Integer resolveMinutes(String policyMode, Integer weekStdMinutes) {

        if (MODE_DEFAULT.equals(policyMode)) {
            return null;
        }
        if (MODE_DIRECT.equals(policyMode)) {
            if (weekStdMinutes == null) {
                throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
            }
            // 값 범위(0 초과 ~ 2400분)는 StdWorkHoursService 가 검증한다(문구 단일 출처).
            return weekStdMinutes;
        }
        throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
    }
}
