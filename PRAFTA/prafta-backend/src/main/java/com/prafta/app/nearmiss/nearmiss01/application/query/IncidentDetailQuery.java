package com.prafta.app.nearmiss.nearmiss01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A5 사건 단건 상세/상태 조회 Query (사업장 스코프 강제).
 * 생성자를 직접 사용해 상태 조회(selectReportStatus 등)에도 재사용한다.
 */
public record IncidentDetailQuery(
    String siteCd
    , String nearMissId
    , String gvCmpnyCd
){
    public IncidentDetailQuery {
        if (gvCmpnyCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
    }
}
