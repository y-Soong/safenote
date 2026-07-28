package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PC-07(D9-③): 소멸 임박 짜투리 리포트 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD 는 JWT 에서만 취득. 관리자(MASTER/HR) 게이트는 서비스 진입부에서
 * {@code gvAuthCd} 로 강제한다(정책서 §8.5.7 — ManualTypesParam 패턴 미러).
 */
public record RemnantReportParam(String gvCmpnyCd, String gvAuthCd) {

    public static RemnantReportParam from(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new RemnantReportParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd());
    }
}
