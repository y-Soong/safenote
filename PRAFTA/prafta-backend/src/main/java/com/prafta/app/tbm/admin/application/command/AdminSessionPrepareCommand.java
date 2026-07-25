package com.prafta.app.tbm.admin.application.command;

/**
 * 교육준비(OPENED) 전이 UPDATE 커맨드(prafta-051 R-A, app 관리자).
 *
 * <p>DRAFT→OPENED 전이 시 입실비번(entryPwd) + 관리자 GPS 중심좌표 + PREP_START_AT(NOW) 를 확정한다.
 * OPENED_AT 은 매퍼에서 IFNULL(OPENED_AT, NOW()) 로 감사 보존(덮어쓰기 금지)한다.
 * 종료비번(EXIT_PWD)은 발급하지 않는다(교육종료 전이 소관).
 *
 * <p>GPS좌표-암호화-전환-06: 좌표는 암호문(managerGpsLatEnc/LonEnc → MANAGER_GPS_LAT_ENC/LON_ENC)만
 * 저장한다. 암호화는 서비스 계층(GpsCoordCrypto.encryptString — trim/빈값 null 처리 포함)에서 수행.
 */
public record AdminSessionPrepareCommand(
    String sessionCd
    , String entryPwd
    , String managerGpsLatEnc   // 개설 위도 암호문(AES-GCM v1.)
    , String managerGpsLonEnc   // 개설 경도 암호문
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionPrepareCommand of(
            String sessionCd, String entryPwd, String managerGpsLatEnc, String managerGpsLonEnc,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionPrepareCommand(
            sessionCd
            , entryPwd
            , managerGpsLatEnc
            , managerGpsLonEnc
            , gvCmpnyCd
            , gvUserCd
        );
    }
}
