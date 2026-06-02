package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 직원별 연차 상세 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD는 JWT에서만 취득. {@code userCd}는 path variable.
 *
 * <p>상세 조회는 특정 직원 PII를 노출하므로 관리자(MASTER/HR) 권한 가드를 강제한다
 * (정책서 §8.5.7). 이를 위해 JWT의 {@code gv_authCd}를 함께 운반하여 서비스 진입부에서 검증한다.
 */
public record LeaveDetailParam(String gvCmpnyCd, String gvAuthCd, String userCd) {

    public static LeaveDetailParam from(String userCd, TokenInfo tokenInfo) {
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
        return new LeaveDetailParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), userCd);
    }
}
