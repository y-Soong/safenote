package com.prafta.web.attd.attd09.application.param;

import java.util.Collections;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.HireDateGrantRequest;

/**
 * 입사일 기준 연차 부여 진입 Param (테스트/검증용).
 *
 * <p>JWT 클레임(권한/회사/수행자)을 함께 운반하여 서비스 계층이 권한 가드 + 스코프 격리를
 * 수행한다(정책서 §8.5.7). cmpnyCd는 JWT에서만 취득(요청 body 미신뢰 — 가드레일 3).
 */
public record HireDateGrantParam(
      List<String> userCds
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {

    public static HireDateGrantParam from(HireDateGrantRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        List<String> userCds = (request.getUserCds() == null)
                ? Collections.emptyList()
                : request.getUserCds();
        return new HireDateGrantParam(
              userCds
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
