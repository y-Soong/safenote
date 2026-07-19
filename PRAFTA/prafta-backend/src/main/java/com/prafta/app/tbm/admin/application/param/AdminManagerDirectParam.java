package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminManagerDirectRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * E10 정규직 관리자 대리입실 파라미터(prafta-051 R-B).
 *
 * <p>sessionCd 는 path, 대상 userCd 는 바디에서 받되 식별자(회사/사용자/사업장/권한)는 JWT 클레임에서만
 * 도출한다(IDOR 차단). 대상 userCd 가 세션 사업장/노드 스코프 내 활성 정규직인지는 서버가 재검증한다.
 */
public record AdminManagerDirectParam(
    String sessionCd
    , String userCd
    // PRAFTA-SUBCON-T5 F9: 대상 회사코드를 받지 않는다(앱 대리입실 = 자사 전용). 회사는 토큰 출처.
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminManagerDirectParam from(
            String sessionCd, AdminManagerDirectRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String userCd = request != null ? request.getUserCd() : null;

        return new AdminManagerDirectParam(
            sessionCd
            , userCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
