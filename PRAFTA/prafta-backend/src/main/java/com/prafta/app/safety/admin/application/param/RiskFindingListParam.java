package com.prafta.app.safety.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * H3 위험성평가 목록 조회 Param.
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임. siteCd 는 리소스 사업장(없으면 토큰 폴백).
 *    assessmentStatus/processCd/riskTypeCd 는 표시 필터(선택). 상태값은 화이트리스트 검증한다.
 */
public record RiskFindingListParam(
      String siteCd
    , String assessmentStatus
    , String processCd
    , String riskTypeCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static RiskFindingListParam of(String siteCd, String assessmentStatus,
            String processCd, String riskTypeCd, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String status = (assessmentStatus == null || assessmentStatus.isBlank()) ? null : assessmentStatus.trim();
        // SYS011 진행상태 화이트리스트(001~004) — 그 외 값은 무필터로 흘려보내지 않고 잘못된 요청 처리.
        if (status != null && !("001".equals(status) || "002".equals(status)
                || "003".equals(status) || "004".equals(status))) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new RiskFindingListParam(
                (siteCd == null || siteCd.isBlank()) ? null : siteCd.trim(),
                status,
                (processCd == null || processCd.isBlank()) ? null : processCd.trim(),
                (riskTypeCd == null || riskTypeCd.isBlank()) ? null : riskTypeCd.trim(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
