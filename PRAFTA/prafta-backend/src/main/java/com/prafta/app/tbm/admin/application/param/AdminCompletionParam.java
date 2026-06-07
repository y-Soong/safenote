package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminCompletionRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R3 T4 개별 이수처리 파라미터. sessionCd/attendanceCd 는 path, 식별자는 JWT 클레임에서만 도출(IDOR 차단).
 */
public record AdminCompletionParam(
    String sessionCd
    , String attendanceCd
    , String completionStatusCd
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminCompletionParam from(String sessionCd, String attendanceCd,
            AdminCompletionRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminCompletionParam(
            sessionCd
            , attendanceCd
            , request.getCompletionStatusCd()
            , request.getReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
