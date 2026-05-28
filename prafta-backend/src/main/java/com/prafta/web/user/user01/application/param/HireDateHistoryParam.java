package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 입사일 변경 이력 조회 파라미터.
 * 대상 userCd는 path variable, 회사 스코프/권한은 토큰에서만 가져온다(IDOR 방지).
 * LeaveInfoParam과 동일 형태이나 의미 구분을 위해 별도 정의한다.
 */
public record HireDateHistoryParam(
    String userCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static HireDateHistoryParam from(String userCd, TokenInfo tokenInfo) {

        if (userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new HireDateHistoryParam(
            userCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
