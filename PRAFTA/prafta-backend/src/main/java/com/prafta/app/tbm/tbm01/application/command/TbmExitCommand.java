package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C2: 출결 UPDATE(종료) Command.
 * <p>EXIT_TYPE_CD='SELF', COMPLETION_STATUS_CD='COMPLETED' 고정(본인 종료).
 * <p>exitSignFileMgmtCd 는 종료 서명 파일코드(D1 필수).
 * <p>WHERE 절은 본인+미종료(EXIT_AT IS NULL) 만 갱신(IDOR/중복종료 차단).
 */
public record TbmExitCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
    , String exitTypeCd
    , String completionStatusCd
    , String exitSignFileMgmtCd
    , String updateNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";
    private static final String EXIT_TYPE_SELF = "SELF";
    private static final String COMPLETION_COMPLETED = "COMPLETED";

    public static TbmExitCommand of(
            String cmpnyCd, String sessionCd, String userCd, String exitSignFileMgmtCd) {

        if (cmpnyCd == null || sessionCd == null || userCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmExitCommand(
            cmpnyCd
            , sessionCd
            , userCd
            , USER_TYPE_REGULAR
            , EXIT_TYPE_SELF
            , COMPLETION_COMPLETED
            , exitSignFileMgmtCd
            , userCd
        );
    }
}
