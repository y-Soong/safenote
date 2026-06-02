package com.prafta.web.nearmiss.nearmiss01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.ReclassifyParam;

/**
 * 채번 조회용: 사업장+당일 기준 최대 SEQ 조회 키.
 * NEAR_MISS_ID = 'NM' + YYYYMMDD + 3자리 SEQ (사업장별 일련).
 */
public record NearMissIdSeqQuery(
    String siteCd
    , String gvCmpnyCd
){
    public static NearMissIdSeqQuery from(ReclassifyParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NearMissIdSeqQuery(
            param.siteCd()
            , param.gvCmpnyCd()
        );
    }
}
