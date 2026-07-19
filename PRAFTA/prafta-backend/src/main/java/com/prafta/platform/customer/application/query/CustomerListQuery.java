package com.prafta.platform.customer.application.query;

import com.prafta.platform.customer.application.param.CustomerListParam;

/**
 * 고객 리스트 조회 쿼리 파라미터(TB_CMPNY 전용).
 */
public record CustomerListQuery(
    String cmpnyNm
    , String contractYn
    , String useYn
) {
    public static CustomerListQuery from(CustomerListParam param) {
        return new CustomerListQuery(
            param.cmpnyNm()
            , param.contractYn()
            , param.useYn()
        );
    }
}
