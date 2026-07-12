package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 시간차 1일 환산시간 조회 Param (LC-02).
 *
 * <p>이력 조회에 권한 가드(AUTH_MASTER OR AUTH_HR_MANAGER — 정책 이력 §8.5.7 미러)가
 * 걸리므로 CMPNY_CD 와 함께 AUTH_CD 도 JWT 에서 도출한다(body 신뢰 금지).
 */
public record LeaveConversionParam(
      String gvCmpnyCd
    , String gvAuthCd
) {

    public static LeaveConversionParam from(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveConversionParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd());
    }
}
