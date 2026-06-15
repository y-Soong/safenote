package com.prafta.common.cmm.dailylogin.application.command;

import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-app-027-2 — 일용직 비밀번호 잠금 해제 명령. 정규 UserPwdUnlockCommand 미러.
 */
public record DailyUserPwdUnlockCommand(
    String cmpnyCd
    , String userCd
) {
    public static DailyUserPwdUnlockCommand from(DailyUserResult userResult) {

        if (userResult == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DailyUserPwdUnlockCommand(
            userResult.cmpnyCd()
            , userResult.userCd()
        );
    }
}
