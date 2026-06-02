package com.prafta.app.attd.attd01.result;

import java.math.BigDecimal;

/**
 * prafta-app-002: 근태 GPS 측정 결과 (TB_USER_ATTD_GPS).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectGpsByAttdIds.
 * <p>GPS_INFO_TYPE[SYS028] 01=출근/02=퇴근. IS_MOCKED Y/N.
 *   ACCURACY 는 m 단위 정확도(임계값 검증용). siteGpsRange 는 TB_SITE.GPS_RANGE(허용반경 m).
 * <p>같은 ATTD_ID + GPS_INFO_TYPE 에 여러 측정행이 있을 수 있어 최신행(API_CALL_DATE/TIME DESC)만 사용.
 */
public record GpsResult(
    String attdId
    , String gpsInfoType
    , String isMocked
    , BigDecimal accuracy
    , String apiCallDate
    , String apiCallTime
    , String siteGpsRange
) {
}
