package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 직원별 연도별 연차 사용 이력 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD는 JWT에서만 취득. {@code userCd}는 path variable,
 * {@code year}는 query parameter(YYYY, 4자리).
 *
 * <p>연차 상세(LeaveDetailParam)와 동일하게 특정 직원 PII를 노출하므로 관리자(MASTER/HR)
 * 권한 가드를 강제한다(정책서 §8.5.7).
 */
public record UsageHistoryParam(String gvCmpnyCd, String gvAuthCd, String userCd, String year) {

    public static UsageHistoryParam from(String userCd, String year, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (year == null || !year.matches("\\d{4}")) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new UsageHistoryParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), userCd, year);
    }
}
