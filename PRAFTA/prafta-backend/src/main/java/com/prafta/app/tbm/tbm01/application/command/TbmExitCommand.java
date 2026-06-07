package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C2: 출결 UPDATE(종료) Command.
 * <p>EXIT_TYPE_CD='SELF', COMPLETION_STATUS_CD='COMPLETED' 고정(본인 종료).
 * <p>exitSignFileMgmtCd 는 종료 서명 파일코드(D1 필수).
 * <p>WHERE 절은 본인+미종료(EXIT_AT IS NULL) 만 갱신(IDOR/중복종료 차단).
 * <p>prafta-051-08: appForegroundSec(앱 포그라운드 누적초, nullable) 추가 —
 *   SELF_DEVICE 본인 종료 출결에만 저장. 음수/과대값은 of() 에서 방어.
 */
public record TbmExitCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
    , String exitTypeCd
    , String completionStatusCd
    , String exitSignFileMgmtCd
    , Integer appForegroundSec
    , String updateNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";
    private static final String EXIT_TYPE_SELF = "SELF";
    private static final String COMPLETION_COMPLETED = "COMPLETED";

    /** prafta-051-08: 포그라운드 누적초 상한(24시간=86400초). 그 이상은 비정상값으로 보고 상한 클램프. */
    private static final int FOREGROUND_SEC_MAX = 86400;

    public static TbmExitCommand of(
            String cmpnyCd, String sessionCd, String userCd,
            String exitSignFileMgmtCd, Integer appForegroundSec) {

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
            , sanitizeForegroundSec(appForegroundSec)
            , userCd
        );
    }

    /**
     * prafta-051-08: 포그라운드 누적초 방어.
     * <p>null → null(BE NULL 저장). 음수 → null(비정상, 보조지표라 0 대신 미상으로 둠).
     *   상한 초과 → 86400 클램프.
     */
    private static Integer sanitizeForegroundSec(Integer sec) {
        if (sec == null) {
            return null;
        }
        if (sec < 0) {
            return null;
        }
        if (sec > FOREGROUND_SEC_MAX) {
            return FOREGROUND_SEC_MAX;
        }
        return sec;
    }
}
