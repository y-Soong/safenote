package com.prafta.platform.customer.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.common.PlatformConstants;
import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.application.param.TokenQuotaUpdateParam;
import com.prafta.platform.customer.application.query.CustomerListQuery;
import com.prafta.platform.customer.application.result.CustomerListResult;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.dto.response.TokenQuotaUpdateResponse;
import com.prafta.platform.customer.mapper.PlatformCustomerMapper;
import com.prafta.platform.customer.service.PlatformCustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformCustomerServiceImpl implements PlatformCustomerService {

    private final PlatformCustomerMapper platformCustomerMapper;

    /** 당월 사용량 조인 키(USE_YM) 기준 타임존 — 쿼터 서비스(AiQuotaServiceImpl)와 동일하게 KST 고정. */
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    /** USE_YM 포맷(YYYYMM). */
    private static final DateTimeFormatter USE_YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public CustomerListResponse selectCustomerList(CustomerListParam param) {

        // 당월(KST) 사용량 조인 키 주입 — 쿼터 판정(AiQuotaServiceImpl.currentUseYm)과 동일 기준.
        String useYm = LocalDate.now(ZONE_KST).format(USE_YM_FMT);
        CustomerListQuery query = CustomerListQuery.from(param, useYm);

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

    @Override
    @Transactional
    public TokenQuotaUpdateResponse updateTokenQuota(TokenQuotaUpdateParam param) {

        // 운영자 자기 자신(prafta_system_admin) 지정 거부 + TB_CMPNY 미존재 거부(모드/값 검증은 Param.from 이 확정).
        if (PlatformConstants.PLATFORM_CMPNY_CD.equals(param.cmpnyCd())
                || platformCustomerMapper.selectCmpnyExists(param.cmpnyCd()) == 0) {
            log.warn("AI 토큰 한도 변경 거부 - 대상 회사 미존재/운영자 자기 지정 - cmpnyCd={}", param.cmpnyCd());
            throw new ApiException(PlatformErrorCode.PLATFORM_400_015);
        }

        platformCustomerMapper.upsertTokenQuota(
            param.cmpnyCd(), param.monthlyTokenLimit(), param.operatorUserCd());

        log.info("AI 토큰 한도 변경 완료 - cmpnyCd={}, limit={}, operator={}",
            param.cmpnyCd(), param.monthlyTokenLimit(), param.operatorUserCd());

        return TokenQuotaUpdateResponse.builder()
                .cmpnyCd(param.cmpnyCd())
                .tokenLimit(param.monthlyTokenLimit())
                .quotaCustomYn("Y")
                .build();
    }
}
