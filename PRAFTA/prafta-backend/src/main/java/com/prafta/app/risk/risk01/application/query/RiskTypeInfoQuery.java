package com.prafta.app.risk.risk01.application.query;

import com.prafta.app.risk.risk01.application.param.RiskTypeInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B2: 위험성평가 구분/분류/발생상황 조회 Query (mapper 진입).
 *
 * <p>3개 SELECT (selectRiskCategory / selectRiskType / selectRiskHazard) 가 공통으로 사용한다.
 *   - selectRiskCategory: siteCd 미사용 (CMPNY 단위)
 *   - selectRiskType / selectRiskHazard: SITE_CD IS NULL OR = #{param.siteCd}
 */
public record RiskTypeInfoQuery(
    String siteCd
) {
    public static RiskTypeInfoQuery from(RiskTypeInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskTypeInfoQuery(
            param.siteCd()
        );
    }
}
