package com.prafta.app.attd.attd01.application.command;

import java.math.BigDecimal;

/**
 * prafta-app-003 A1: 출근 GPS INSERT 커맨드 (TB_USER_ATTD_GPS).
 *
 * <p>GPS_ID 는 매퍼에서 selectGpsId 로 선채번하여 주입한다.
 *   GPS_INFO_TYPE='01'(출근, SYS028). SITE_CD 는 출근 레코드의 SITE_CD(세션 siteCd) 를 사용한다.
 *   API_CALL_DATE/TIME 은 서버 시각(YYYYMMDD / HHmmss). INSERT_NO=userCd.
 *   퇴근(CheckOutGpsCommand)과 동일 형태이며 GPS_INFO_TYPE 만 '01' 로 다르다.
 *
 * <p>GPS좌표-암호화-전환-02: 좌표는 평문(LAT/LON) 대신 AES-GCM 암호문(latEnc/lonEnc → LAT_ENC/LON_ENC)만
 *   저장한다. 암호화는 서비스 계층(GpsCoordCrypto — scale 7 정규화 후 encrypt)에서 수행한다.
 */
public record CheckInGpsCommand(
    String gpsId
    , String cmpnyCd
    , String attdId
    , String siteCd
    , String userCd
    , String gpsInfoType   // '01' (출근)
    , String latEnc        // 위도 암호문(AES-GCM v1.)
    , String lonEnc        // 경도 암호문(AES-GCM v1.)
    , BigDecimal accuracy
    , String apiCallDate   // YYYYMMDD
    , String apiCallTime   // HHmmss
    , String isMocked      // 'Y'/'N'
    , String ipAddr
    , String offsiteReason // prafta-app-008: 외근 사유(외근 GPS 행에만 저장, NULL 불가 아님)
    , String insertNo      // userCd
) {
}
