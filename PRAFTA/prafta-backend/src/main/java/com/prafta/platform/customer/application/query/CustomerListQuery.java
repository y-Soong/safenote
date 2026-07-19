package com.prafta.platform.customer.application.query;

import com.prafta.platform.customer.application.param.CustomerListParam;

/**
 * 고객 리스트 조회 쿼리 파라미터(TB_CMPNY 전용).
 *
 * <p>useYm: 당월 AI 토큰 사용량(TB_AI_TOKEN_USAGE) 조인 키(YYYYMM) — 서비스가 KST 로 계산해 주입.
 */
public record CustomerListQuery(
    String cmpnyNm
    , String contractYn
    , String useYn
    , String useYm
) {
    public static CustomerListQuery from(CustomerListParam param, String useYm) {
        return new CustomerListQuery(
            param.cmpnyNm()
            , param.contractYn()
            , param.useYn()
            , useYm
        );
    }
}
