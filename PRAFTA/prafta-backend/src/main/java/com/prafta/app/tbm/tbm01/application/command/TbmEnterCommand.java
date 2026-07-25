package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C1: 출결 INSERT(입실) Command.
 * <p>ATTENDANCE_CD 는 SQL 에서 'A'+YYYYMMDD+SEQ 채번(FNC_CMM_SEQ_NEXTVAL).
 * <p>GPS좌표-암호화-전환-07: 좌표는 평문(ENTRY_GPS_LAT/LON) 대신 암호문(latEnc/lonEnc →
 *   ENTRY_GPS_LAT_ENC/LON_ENC)만 저장한다. 암호화는 서비스 계층(GpsCoordCrypto —
 *   BigDecimal.valueOf(double) 경유 scale 7 정규화)에서 수행하며, 거리판정은 암호화 전 원본
 *   Double 로 기존 위치에서 수행한다. 좌표/암호문은 응답/로그에 노출하지 않는다(D5).
 * <p>distanceM 은 거리(m, null 허용). ENTRY_TYPE_CD='SELF_DEVICE', USER_TYPE_CD='REGULAR' 고정(MVP).
 */
public record TbmEnterCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
    , String entryTypeCd
    , String latEnc        // 입실 위도 암호문(AES-GCM v1., 측위 실패 시 null)
    , String lonEnc        // 입실 경도 암호문
    , Integer distanceM
    , String insertNo
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";
    private static final String ENTRY_TYPE_SELF_DEVICE = "SELF_DEVICE";

    public static TbmEnterCommand of(
            String cmpnyCd, String sessionCd, String userCd,
            String latEnc, String lonEnc, Integer distanceM) {

        if (cmpnyCd == null || sessionCd == null || userCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmEnterCommand(
            cmpnyCd
            , sessionCd
            , userCd
            , USER_TYPE_REGULAR
            , ENTRY_TYPE_SELF_DEVICE
            , latEnc
            , lonEnc
            , distanceM
            , userCd
        );
    }
}
