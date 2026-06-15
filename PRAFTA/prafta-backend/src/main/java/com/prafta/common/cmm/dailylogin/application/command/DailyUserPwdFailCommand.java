package com.prafta.common.cmm.dailylogin.application.command;

import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-app-027-2 — 일용직 비밀번호 실패 누적/잠금 명령. 정규 UserPwdFailCommand 미러.
 */
public record DailyUserPwdFailCommand(
    String cmpnyCd
    , String userCd
    , int lockDurationMinutes
    , int pwdFailMaxCnt
) {
    public static DailyUserPwdFailCommand from(DailyUserResult userResult, int lockDurationMinutes, int pwdFailMaxCnt) {

        if (userResult == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DailyUserPwdFailCommand(
            userResult.cmpnyCd()
            , userResult.userCd()
            , lockDurationMinutes
            , pwdFailMaxCnt
        );
    }
}
