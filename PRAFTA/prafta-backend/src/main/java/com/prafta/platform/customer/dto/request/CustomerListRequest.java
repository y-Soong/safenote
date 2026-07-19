package com.prafta.platform.customer.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 리스트 조회 요청 DTO (GET /platformApi/customer/customer-lists).
 *
 * <p>모든 조건은 선택이며, 미입력 시 전체 조회(단, 서버측 LIMIT 500 고정).
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerListRequest {

    /** 회사명(부분일치 LIKE). */
    private String cmpnyNm;

    /** 계약여부(Y/N, 미입력=전체). */
    private String contractYn;

    /** 사용여부(Y/N, 미입력=전체). */
    private String useYn;
}
