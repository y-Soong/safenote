package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 입사일 변경 영향 분석 파라미터 (PRAFTA-017-4, 근사치).
 * 대상 userCd는 path variable, 변경할 입사일은 query string, 회사 스코프/권한은 토큰에서만 가져온다.
 */
public record HireDateImpactParam(
    String userCd
    , String newHireDate   /** YYYYMMDD */
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static HireDateImpactParam from(String userCd, String newDate, TokenInfo tokenInfo) {

        if (userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // YYYY-MM-DD -> YYYYMMDD 정규화
        String date = newDate;
        if (date != null) {
            date = date.replace("-", "");
        }

        return new HireDateImpactParam(
            userCd
            , date
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
