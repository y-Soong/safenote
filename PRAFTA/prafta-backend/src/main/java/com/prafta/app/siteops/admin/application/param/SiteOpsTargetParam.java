package com.prafta.app.siteops.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 현장 처리 계약서 서명 플로우(메타/페이지/서명) 대상 파라미터.
 *
 * <p>QR 스캔은 출근(check-in) 시 1회만 수행하고, 그 응답(SIGN_REQUIRED)의 userCd 를 후속 호출의
 * 리소스 키로 쓴다. 서버는 targetUserCd 를 신뢰하지 않고 매 호출마다 관리자 진입 게이트·사업장
 * 멤버십·대상 유효성(활성 일용직 + 슬롯 점유)을 QR 경로와 동일하게 재검증한다(IDOR 차단).
 */
public record SiteOpsTargetParam(
    String targetUserCd
    , String reqSiteCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    public static SiteOpsTargetParam from(String targetUserCd, String siteCd, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (targetUserCd == null || targetUserCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new SiteOpsTargetParam(
            targetUserCd
            , siteCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
