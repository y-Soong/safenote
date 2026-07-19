package com.prafta.platform.location.application.query;

import com.prafta.platform.location.application.param.GpsListParam;

/**
 * 위치정보 UNION 조회 쿼리 파라미터(TB_USER_ATTD_GPS + TB_TBM_ATTENDANCE).
 */
public record GpsListQuery(
    String cmpnyCd
    , String siteCd
    , String date
) {
    public static GpsListQuery from(GpsListParam param) {
        return new GpsListQuery(
            param.cmpnyCd()
            , param.siteCd()
            , param.date()
        );
    }
}
