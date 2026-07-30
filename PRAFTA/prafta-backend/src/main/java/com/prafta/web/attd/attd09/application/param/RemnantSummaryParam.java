package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PC-07(D9-②): 회사 부담 보전 연간 집계 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD 는 JWT 에서만 취득. 관리자(MASTER/HR) 게이트는 서비스 진입부에서
 * {@code gvAuthCd} 로 강제한다(정책서 §8.5.7 — ManualTypesParam 패턴 미러).
 *
 * @param year 집계 연도(YYYY). null/형식 불일치면 서비스가 올해로 폴백.
 */
public record RemnantSummaryParam(String year, String gvCmpnyCd, String gvAuthCd) {

    public static RemnantSummaryParam from(String year, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new RemnantSummaryParam(year, tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd());
    }
}
