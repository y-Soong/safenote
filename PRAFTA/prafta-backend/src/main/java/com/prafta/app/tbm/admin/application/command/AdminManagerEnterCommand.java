package com.prafta.app.tbm.admin.application.command;

/**
 * 관리자 입실 INSERT 커맨드(prafta-051, web Tbm02 insertManagerEntry 포팅).
 *
 * <p>R-B 정규직 대리입실(userTypeCd='REGULAR', entryTypeCd='MANAGER_DIRECT')과 R-D 일용직 QR 입실
 * (userTypeCd='DAILY', entryTypeCd='MANAGER_QR_SCAN')이 동일 INSERT 를 공유하며, userTypeCd/entryTypeCd
 * 만 분기한다(SYS051 입실경로).
 *
 * <p>TB_TBM_ATTENDANCE 신규 행: ENTRY_BY_MANAGER_USER_CD=관리자(토큰), ENTRY_AT=NOW(). ENTRY_GPS_LAT/LON 은
 * 세션의 MANAGER_GPS_LAT/LON 을 감사용으로 복사(매퍼 INSERT...SELECT), ENTRY_DISTANCE_M 은 NULL(거리검증 안 함, D-4).
 * UK(CMPNY,SESSION,USER_TYPE,USER) 멱등.
 */
public record AdminManagerEnterCommand(
    String gvCmpnyCd
    , String sessionCd
    , String userTypeCd
    , String userCd
    , String entryTypeCd
    , String managerUserCd
){
    public static AdminManagerEnterCommand of(
            String gvCmpnyCd, String sessionCd, String userTypeCd, String userCd,
            String entryTypeCd, String managerUserCd) {
        return new AdminManagerEnterCommand(
                gvCmpnyCd, sessionCd, userTypeCd, userCd, entryTypeCd, managerUserCd);
    }
}
