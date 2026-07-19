package com.prafta.platform.customer.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.application.query.CustomerListQuery;
import com.prafta.platform.customer.application.result.CustomerListResult;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.mapper.PlatformCustomerMapper;
import com.prafta.platform.customer.service.PlatformCustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformCustomerServiceImpl implements PlatformCustomerService {

    private final PlatformCustomerMapper platformCustomerMapper;

    @Override
    public CustomerListResponse selectCustomerList(CustomerListParam param) {

        CustomerListQuery query = CustomerListQuery.from(param);

        // LIMIT 500 고정 목록 + 전체 건수(절단 판정) — 둘 다 read-only.
        List<CustomerListResult> customerList = platformCustomerMapper.selectCustomerList(query);
        int totalCnt = platformCustomerMapper.selectCustomerListCount(query);

        boolean truncated = totalCnt > customerList.size();
        if (truncated) {
            log.info("고객 리스트 절단 - 전체 {}건 중 {}건 반환(LIMIT 500)", totalCnt, customerList.size());
        }

        return CustomerListResponse.builder()
                .customerList(customerList)
                .totalCnt(totalCnt)
                .truncated(truncated)
                .build();
    }
}
