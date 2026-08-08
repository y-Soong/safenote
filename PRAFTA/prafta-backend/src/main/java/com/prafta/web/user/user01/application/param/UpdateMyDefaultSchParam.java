package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UpdateMyDefaultSchRequest;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 Param(웹).
 *
 * <p>대상 회사/사용자는 세션 토큰(gv_cmpnyCd/gv_userCd)에서만 도출한다(IDOR 방지).
 * 사업장(SITE_CD)은 파라미터로 받지 않는다 — 본인 사업장 변경은 소속이동 전용.
 */
public record UpdateMyDefaultSchParam(
      String cmpnyCd
    , String userCd
    , String defaultSchCd
) {
    public static UpdateMyDefaultSchParam from(UpdateMyDefaultSchRequest request, TokenInfo tokenInfo) {
        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 본인 기본 근무타입 변경: 대상 회사/사용자 식별자는 토큰 값으로 강제한다(IDOR 방지).
        return new UpdateMyDefaultSchParam(
              tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , request.getDefaultSchCd()
        );
    }
}
