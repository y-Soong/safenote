package com.prafta.platform.customer.service;

import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.dto.response.CustomerListResponse;

/**
 * 플랫폼 고객 리스트 서비스(Platform_03 — read-only).
 */
public interface PlatformCustomerService {

    /** 고객사 목록 조회(검색: 회사명 부분일치/계약여부/사용여부, LIMIT 500 + 전체 건수). */
    CustomerListResponse selectCustomerList(CustomerListParam param);
}
