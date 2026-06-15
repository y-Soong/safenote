package com.prafta.app.siteops.admin.application.param;

import com.prafta.app.siteops.admin.dto.request.SiteOpsQrRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 파라미터.
 *
 * <p>식별자(회사/사용자/권한)는 JWT 클레임에서만 도출한다(IDOR 차단). qrPayload 와 현장전환 siteCd 만
 * 바디에서 받으며, siteCd 는 서버가 접근가능 사업장(USE_YN='Y') 멤버십으로 재검증한 뒤 권위로 사용한다.
 */
public record SiteOpsQrParam(
    String qrPayload
    , String reqSiteCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static SiteOpsQrParam from(SiteOpsQrRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String qrPayload = request != null ? request.getQrPayload() : null;
        String reqSiteCd = request != null ? request.getSiteCd() : null;

        return new SiteOpsQrParam(
            qrPayload
            , reqSiteCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
