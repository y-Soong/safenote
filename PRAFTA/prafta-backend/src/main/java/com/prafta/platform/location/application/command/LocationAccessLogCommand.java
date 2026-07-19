package com.prafta.platform.location.application.command;

/**
 * 위치정보 열람 로그(TB_LOCATION_ACCESS_LOG) INSERT 커맨드 — append-only.
 *
 * <p>열람일시(ACCESS_DTIME)/입력일시는 서버 NOW() 로 XML 에서 기록한다(클라이언트 시각 불신).
 *
 * <p>IP 두 값의 의미 구분(V-1):
 * clientIp = 해석값(신뢰 프록시 경유 시 XFF 선두, 그 외 RemoteAddr) /
 * remoteAddr = 직접 연결 IP 원시값(XFF 위조 대비 병기).
 */
public record LocationAccessLogCommand(
    String accessorUserCd
    , String targetCmpnyCd
    , String targetSiteCd
    , String targetDate
    , Long smsAuthId
    , String smsVerifiedAt
    , int resultCnt
    , String clientIp
    , String remoteAddr
) {
}
