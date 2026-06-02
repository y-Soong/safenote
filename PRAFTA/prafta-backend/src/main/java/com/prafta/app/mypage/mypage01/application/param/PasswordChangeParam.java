package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.PasswordChangeRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-04: 비밀번호 변경 Param.
 */
public record PasswordChangeParam(
      String currentPassword
    , String newPassword
    , TokenInfo tokenInfo
) {
    public static PasswordChangeParam from(PasswordChangeRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new PasswordChangeParam(
              request.getCurrentPassword()
            , request.getNewPassword()
            , tokenInfo
        );
    }
}
