package com.prafta.platform.customer.application.param;

import com.prafta.platform.customer.dto.request.CustomerListRequest;

/**
 * 고객 리스트 조회 파라미터.
 *
 * <p>read-only 조회라 운영자 식별자는 불필요(인가는 PlatformOperatorGateInterceptor 가 강제).
 * 빈 문자열 조건은 null 로 정규화하여 매퍼 동적 조건을 단순화한다.
 */
public record CustomerListParam(
    String cmpnyNm
    , String contractYn
    , String useYn
) {
    public static CustomerListParam from(CustomerListRequest request) {

        if (request == null) {
            return new CustomerListParam(null, null, null);
        }

        return new CustomerListParam(
            blankToNull(request.getCmpnyNm())
            , blankToNull(request.getContractYn())
            , blankToNull(request.getUseYn())
        );
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
