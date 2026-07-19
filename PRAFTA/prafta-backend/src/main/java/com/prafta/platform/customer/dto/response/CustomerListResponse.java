package com.prafta.platform.customer.dto.response;

import java.util.List;

import com.prafta.platform.customer.application.result.CustomerListResult;

import lombok.Builder;
import lombok.Value;

/**
 * 고객 리스트 조회 응답 DTO.
 *
 * <p>주소 결합(ADDR_1 + ADDR_2)은 프론트에서 수행한다(원본 컬럼 그대로 응답).
 */
@Value
@Builder
public class CustomerListResponse {

    /** 고객사 목록(LIMIT 500 적용분). */
    List<CustomerListResult> customerList;

    /** 검색 조건 일치 전체 건수(LIMIT 무관 — 별도 COUNT). */
    int totalCnt;

    /** LIMIT 500 초과 절단 여부(true 면 프론트가 안내 배너 노출). */
    boolean truncated;
}
