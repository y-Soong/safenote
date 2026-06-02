package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C(D4): 비밀번호 실패 로그 INSERT Command.
 * <p>tb_tbm_pwd_fail — 평문 비밀번호는 저장하지 않는다(시각/유형/시도자만, 스키마 헤더 보안 권고).
 * <p>USER_TYPE_CD='REGULAR' 고정(MVP).
 */
public record TbmPwdFailCommand(
    String cmpnyCd
    , String sessionCd
    , String pwdTypeCd      // SYS055 ENTRY/EXIT
    , String userTypeCd
    , String userCd
    , String insertNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    public static TbmPwdFailCommand of(String cmpnyCd, String sessionCd, String pwdTypeCd, String userCd) {

        if (cmpnyCd == null || sessionCd == null || pwdTypeCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmPwdFailCommand(
            cmpnyCd
            , sessionCd
            , pwdTypeCd
            , USER_TYPE_REGULAR
            , userCd
            , userCd
        );
    }
}
