package com.prafta.app.safety.admin.application.param;

import com.prafta.app.safety.admin.dto.request.RiskStatusRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * H5 위험성평가 상태전환 Param.
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임에서만 도출(IDOR). siteCd 미전달 시 토큰 gv_siteCd 폴백.
 *    리소스 키(processCd/assessmentCd)/targetStatus 공백은 잘못된 요청(COMMON_400_001).
 *    상세 전이/입력 검증은 서비스에서 수행(전이표·동시성 가드).
 */
public record RiskStatusParam(
      String siteCd
    , String processCd
    , String assessmentCd
    , String targetStatus
    , String revalDate
    , String revalBeforeDesc
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static RiskStatusParam from(RiskStatusRequest req, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (req == null
                || isBlank(req.getProcessCd())
                || isBlank(req.getAssessmentCd())
                || isBlank(req.getTargetStatus())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new RiskStatusParam(
                isBlank(req.getSiteCd()) ? null : req.getSiteCd().trim(),
                req.getProcessCd().trim(),
                req.getAssessmentCd().trim(),
                req.getTargetStatus().trim(),
                isBlank(req.getRevalDate()) ? null : req.getRevalDate().trim(),
                isBlank(req.getRevalBeforeDesc()) ? null : req.getRevalBeforeDesc().trim(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
