package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm-A9: 중도퇴실(미이수 종료) Command.
 *
 * <p>EXIT_TYPE_CD='SELF', COMPLETION_STATUS_CD='NOT_COMPLETED', EXIT_AT=NOW() 로 갱신한다.
 *   서명/비번/사유 없음(NOT_COMPLETED_REASON NULL 허용). 정상 종료(updateExit, COMPLETED)와 구분.
 * <p>WHERE: 본인(REGULAR+USER_CD) + 미종료(EXIT_AT IS NULL) + DEL_YN='N' (멱등/IDOR 가드).
 */
public record TbmWithdrawCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
    , String exitTypeCd
    , String completionStatusCd
    , String updateNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";
    private static final String EXIT_TYPE_SELF = "SELF";
    private static final String COMPLETION_NOT_COMPLETED = "NOT_COMPLETED";

    public static TbmWithdrawCommand of(String cmpnyCd, String sessionCd, String userCd) {

        if (cmpnyCd == null || sessionCd == null || userCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmWithdrawCommand(
            cmpnyCd
            , sessionCd
            , userCd
            , USER_TYPE_REGULAR
            , EXIT_TYPE_SELF
            , COMPLETION_NOT_COMPLETED
            , userCd
        );
    }
}
