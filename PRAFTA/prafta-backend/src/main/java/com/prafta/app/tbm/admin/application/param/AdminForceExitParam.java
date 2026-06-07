package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminForceExitRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R3 T3 강제 퇴실 파라미터. sessionCd/attendanceCd 는 path, 식별자는 JWT 클레임에서만 도출(IDOR 차단).
 *
 * <p>reason 은 nullable(공백 허용). 빈문자는 서비스에서 NULL 로 정규화한다.
 */
public record AdminForceExitParam(
    String sessionCd
    , String attendanceCd
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminForceExitParam from(String sessionCd, String attendanceCd,
            AdminForceExitRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // reason 은 선택값(nullable). request 자체가 없으면 사유 미입력으로 간주한다.
        String reason = request != null ? request.getReason() : null;

        return new AdminForceExitParam(
            sessionCd
            , attendanceCd
            , reason
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
