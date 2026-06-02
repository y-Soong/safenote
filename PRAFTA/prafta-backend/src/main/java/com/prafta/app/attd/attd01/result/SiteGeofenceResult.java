package com.prafta.app.attd.attd01.result;

import java.math.BigDecimal;

/**
 * prafta-app-003 A0-2: 사업장 지오펜스 기준값 (TB_SITE).
 *
 * <p>중심좌표(LAT/LON, decimal(10,7))와 허용반경(GPS_RANGE, varchar→m).
 *   LAT/LON 또는 GPS_RANGE 가 NULL/빈값이면 거리판정 불가 → 서비스에서 온사이트(정상)로 폴백(A안).
 */
public record SiteGeofenceResult(
    BigDecimal lat
    , BigDecimal lon
    , String gpsRange   // varchar, m. 숫자 변환 필요(빈값/NULL=폴백).
) {
}
