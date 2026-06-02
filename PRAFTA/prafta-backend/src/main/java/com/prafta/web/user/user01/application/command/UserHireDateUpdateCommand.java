package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserHireDateParam;

/**
 * 입사일(HIRE_DATE) UPDATE 커맨드 (PRAFTA-017-4).
 */
public record UserHireDateUpdateCommand(
    String cmpnyCd
    , String userCd
    , String newHireDate   /** YYYYMMDD */
    , String gvUserCd
) {
    public static UserHireDateUpdateCommand from(UserHireDateParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserHireDateUpdateCommand(
            param.gvCmpnyCd()
            , param.userCd()
            , param.newHireDate()
            , param.gvUserCd()
        );
    }
}
