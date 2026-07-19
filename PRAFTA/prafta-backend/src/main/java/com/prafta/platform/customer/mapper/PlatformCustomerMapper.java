package com.prafta.platform.customer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.platform.customer.application.query.CustomerListQuery;
import com.prafta.platform.customer.application.result.CustomerListResult;

/**
 * 플랫폼 고객 리스트 전용 매퍼(TB_CMPNY SELECT 전용 — 쓰기 statement 금지).
 */
@Mapper
public interface PlatformCustomerMapper {

    /** 고객사 목록 조회(LIMIT 500 고정, 운영자 자기 자신 제외). */
    List<CustomerListResult> selectCustomerList(CustomerListQuery query);

    /** 검색 조건 일치 전체 건수(LIMIT 무관 — 절단 판정용). */
    int selectCustomerListCount(CustomerListQuery query);
}
