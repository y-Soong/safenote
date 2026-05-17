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
) {
}
