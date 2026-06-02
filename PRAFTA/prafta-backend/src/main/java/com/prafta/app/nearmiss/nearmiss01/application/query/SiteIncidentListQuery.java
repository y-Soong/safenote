package com.prafta.app.nearmiss.nearmiss01.application.query;

import com.prafta.app.nearmiss.nearmiss01.application.param.SiteIncidentListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A3 사업장 사건 목록 / A4 상태 카운트 조회 Query.
 */
public record SiteIncidentListQuery(
    String siteCd
    , String reportStatusCd
    , String incidentTypeCd
    , String potentialSeverityCd
    , String startDate
    , String endDate
    , String gvCmpnyCd
){
    public static SiteIncidentListQuery from(SiteIncidentListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteIncidentListQuery(
            param.siteCd()
            , param.reportStatusCd()
            , param.incidentTypeCd()
            , param.potentialSeverityCd()
            , param.startDate()
            , param.endDate()
            , param.gvCmpnyCd()
        );
    }
}
