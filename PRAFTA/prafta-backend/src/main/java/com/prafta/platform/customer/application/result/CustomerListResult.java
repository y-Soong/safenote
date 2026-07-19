package com.prafta.platform.customer.application.result;

/**
 * 고객 리스트 조회 결과 1행(TB_CMPNY).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서에 의존한다 — 본 컴포넌트 순서와
 * PlatformCustomerMapper.xml selectCustomerList 의 SELECT 순서를 항상 일치시킬 것.
 */
public record CustomerListResult(
    String cmpnyCd
    , String cmpnyNm
    , String bsnsLcnNo
    , String addr1
    , String addr2
    , String zipCode
    , String contractYn
    , String contractEndDate
    , String useYn
) {
}
