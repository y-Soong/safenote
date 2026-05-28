package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserCreditParam;

/**
 * 경력 인정 전량 소프트 삭제 커맨드 (PRAFTA-017-4).
 * delete-and-insert 패턴의 delete 단계에서 USE_YN='N' 처리한다.
 */
public record UserCreditDeleteCommand(
    String cmpnyCd
    , String userCd
    , String gvUserCd
) {
    public static UserCreditDeleteCommand from(UserCreditParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserCreditDeleteCommand(
            param.gvCmpnyCd()
            , param.userCd()
            , param.gvUserCd()
        );
    }
}
