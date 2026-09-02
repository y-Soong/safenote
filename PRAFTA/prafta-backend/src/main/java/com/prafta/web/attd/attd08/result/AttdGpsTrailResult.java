package com.prafta.web.attd.attd08.result;

import java.math.BigDecimal;

public record AttdGpsTrailResult(
      String gpsId
    , BigDecimal lat
    , BigDecimal lon
    , BigDecimal accuracy
    , String apiCallDate
    , String apiCallTime
    , String isMocked
    , String gpsInfoType
    // S5: 좌표 파기 사유[WITHDRAW/RETENTION]. NULL = 미파기.
    //   화면은 lat/lon 이 null 일 때 이 값으로 배지를 구분해 그린다.
    , String gpsPurgeReasonCd
) {
}
