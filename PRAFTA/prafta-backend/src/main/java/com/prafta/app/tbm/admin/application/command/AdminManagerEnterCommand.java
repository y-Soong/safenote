package com.prafta.app.tbm.admin.application.command;

/**
 * 관리자 입실 INSERT 커맨드(prafta-051, web Tbm02 insertManagerEntry 포팅 + PRAFTA-SUBCON-T5).
 *
 * <p>R-B 정규직 대리입실(userTypeCd='REGULAR', entryTypeCd='MANAGER_DIRECT')과 R-D 일용직 QR 입실
 * (userTypeCd='DAILY', entryTypeCd='MANAGER_QR_SCAN')이 동일 INSERT 를 공유하며, userTypeCd/entryTypeCd
 * 만 분기한다(SYS051 입실경로).
 *
 * <p><b>T5 핵심</b>: 출결행의 CMPNY_CD 와 ATTENDANCE_CD 채번 대상은 <b>참석자 회사
 * ({@code targetCmpnyCd})</b>다(관리자 회사가 아니다). INSERT...SELECT 의 세션 가드는
 * <b>개설사({@code hostCmpnyCd}, 공통 게이트가 돌려준 값)</b> 기준으로 건다.
 *
 * <p>TB_TBM_ATTENDANCE 신규 행: ENTRY_BY_MANAGER_USER_CD=관리자(토큰, 개설사 소속), ENTRY_AT=NOW().
 * ENTRY_GPS_LAT/LON 은 세션의 MANAGER_GPS_LAT/LON 을 감사용으로 복사(매퍼 INSERT...SELECT),
 * ENTRY_DISTANCE_M 은 NULL(거리검증 안 함, D-4). UK(CMPNY,SESSION,USER_TYPE,USER) 멱등.
 */
public record AdminManagerEnterCommand(
    String targetCmpnyCd
    , String hostCmpnyCd
    , String sessionCd
    , String userTypeCd
    , String userCd
    , String entryTypeCd
    , String managerUserCd
){
    public static AdminManagerEnterCommand of(
            String targetCmpnyCd, String hostCmpnyCd, String sessionCd, String userTypeCd,
            String userCd, String entryTypeCd, String managerUserCd) {
        return new AdminManagerEnterCommand(
                targetCmpnyCd, hostCmpnyCd, sessionCd, userTypeCd, userCd, entryTypeCd, managerUserCd);
    }
}
