package com.prafta.app.safety.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * H4 위험성평가 상세 단건 조회 Param.
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임. siteCd/processCd/assessmentCd 는 리소스 키이며
 *    서버가 CMPNY+SITE WHERE 로 스코프 강제(스코프 밖이면 404 — IDOR 차단). siteCd 미전달 시 토큰 폴백.
 */
public record RiskDetailParam(
      String siteCd
    , String processCd
    , String assessmentCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static RiskDetailParam of(String siteCd, String processCd, String assessmentCd, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (isBlank(processCd) || isBlank(assessmentCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new RiskDetailParam(
                (siteCd == null || siteCd.isBlank()) ? null : siteCd.trim(),
                processCd.trim(),
                assessmentCd.trim(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
