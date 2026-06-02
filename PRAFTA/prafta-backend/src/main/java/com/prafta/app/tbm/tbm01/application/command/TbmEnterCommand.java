package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C1: 출결 INSERT(입실) Command.
 * <p>ATTENDANCE_CD 는 SQL 에서 'A'+YYYYMMDD+SEQ 채번(FNC_CMM_SEQ_NEXTVAL).
 * <p>좌표(lat/lon)는 DB 저장만 하고 응답/로그에는 노출하지 않는다(D5). distanceM 은 거리(m, null 허용).
 * <p>ENTRY_TYPE_CD='SELF_DEVICE', USER_TYPE_CD='REGULAR' 고정(MVP).
 */
public record TbmEnterCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
    , String entryTypeCd
    , Double lat
    , Double lon
    , Integer distanceM
    , String insertNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";
    private static final String ENTRY_TYPE_SELF_DEVICE = "SELF_DEVICE";

    public static TbmEnterCommand of(
            String cmpnyCd, String sessionCd, String userCd,
            Double lat, Double lon, Integer distanceM) {

        if (cmpnyCd == null || sessionCd == null || userCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmEnterCommand(
            cmpnyCd
            , sessionCd
            , userCd
            , USER_TYPE_REGULAR
            , ENTRY_TYPE_SELF_DEVICE
            , lat
            , lon
            , distanceM
            , userCd
        );
    }
}
