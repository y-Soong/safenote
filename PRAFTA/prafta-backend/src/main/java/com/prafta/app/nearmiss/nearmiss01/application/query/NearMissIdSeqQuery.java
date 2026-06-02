package com.prafta.app.nearmiss.nearmiss01.application.query;

import com.prafta.app.nearmiss.nearmiss01.application.param.ReportParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 채번 조회용: 사업장+당일 기준 최대 SEQ 조회 키.
 * NEAR_MISS_ID = 'NM' + YYYYMMDD + 3자리 SEQ (사업장별 일련).
 * 웹 selectNextNearMissId 와 동일 SQL 을 앱 mapper 에 미러링.
 */
public record NearMissIdSeqQuery(
    String siteCd
    , String gvCmpnyCd
){
    public static NearMissIdSeqQuery from(ReportParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NearMissIdSeqQuery(
            param.tokenInfo().gv_siteCd()
            , param.tokenInfo().gv_cmpnyCd()
        );
    }
}
