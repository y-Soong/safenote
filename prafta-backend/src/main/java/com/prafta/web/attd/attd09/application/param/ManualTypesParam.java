package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 수동 부여 가능 휴가 종류 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD는 JWT에서만 취득.
 *
 * <p>수동 부여 화면(관리자 전용)의 보조 조회이므로 관리자(MASTER/HR) 권한 가드를 강제한다
 * (정책서 §8.5.7). 이를 위해 JWT의 {@code gv_authCd}를 함께 운반하여 서비스 진입부에서 검증한다.
 */
public record ManualTypesParam(String gvCmpnyCd, String gvAuthCd) {

    public static ManualTypesParam from(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ManualTypesParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd());
    }
}
