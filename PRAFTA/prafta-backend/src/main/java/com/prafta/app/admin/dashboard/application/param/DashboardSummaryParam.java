package com.prafta.app.admin.dashboard.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * J1-10 (B-5): 관리자 대시보드 요약 조회 Param.
 *
 * <p>식별자(cmpny/user/site/auth)는 JWT 클레임에서만 도출한다(IDOR 차단). 현장전환 대응 siteCd(선택)만
 * 클라 입력으로 받되, 서비스에서 멤버십을 재검증한다(J1-6 reqSiteCd 동형).
 */
public record DashboardSummaryParam(
      String siteCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static DashboardSummaryParam of(String siteCd, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new DashboardSummaryParam(
                (siteCd == null || siteCd.isBlank()) ? null : siteCd.trim(),
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
