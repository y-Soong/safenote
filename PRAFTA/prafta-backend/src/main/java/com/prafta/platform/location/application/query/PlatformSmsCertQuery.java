package com.prafta.platform.location.application.query;

/**
 * 플랫폼 위치열람 목적 SMS 인증번호 매칭 쿼리 파라미터.
 *
 * <p>PURPOSE_CD='PLATFORM_LOCATION' 필터는 XML 에 고정(셀프가입 코드와 혼용 차단).
 */
public record PlatformSmsCertQuery(
    String mblNoHmac
    , String certNo
) {
}
