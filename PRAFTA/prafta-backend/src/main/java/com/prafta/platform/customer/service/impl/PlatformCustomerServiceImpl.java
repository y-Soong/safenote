package com.prafta.platform.customer.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.common.PlatformConstants;
import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.application.param.StdWorkPolicyUpdateParam;
import com.prafta.platform.customer.application.param.TokenQuotaUpdateParam;
import com.prafta.platform.customer.application.query.CustomerListQuery;
import com.prafta.platform.customer.application.result.CustomerListResult;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.dto.response.StdWorkPolicyUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenQuotaUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenUsageListResponse;
import com.prafta.platform.customer.mapper.PlatformCustomerMapper;
import com.prafta.platform.customer.service.PlatformCustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformCustomerServiceImpl implements PlatformCustomerService {

    private final PlatformCustomerMapper platformCustomerMapper;

    /** 통상근로시간 기준값 저장·검증 단일 출처(값 범위·법정 상한 문구 포함). */
    private final StdWorkHoursService stdWorkHoursService;

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

    @Override
    @Transactional
    public StdWorkPolicyUpdateResponse updateStdWorkPolicy(StdWorkPolicyUpdateParam param) {

        // 대상 검증은 AI 토큰 한도 변경과 동일 기준 — 운영자 자기 자신(prafta_system_admin) 지정 거부
        // + TB_CMPNY 미존재 거부. 값 범위 검증은 StdWorkHoursService 가 수행한다(문구 단일 출처).
        if (PlatformConstants.PLATFORM_CMPNY_CD.equals(param.cmpnyCd())
                || platformCustomerMapper.selectCmpnyExists(param.cmpnyCd()) == 0) {
            log.warn("통상근로시간 기준값 변경 거부 - 대상 회사 미존재/운영자 자기 지정 - cmpnyCd={}", param.cmpnyCd());
            throw new ApiException(PlatformErrorCode.PLATFORM_400_015);
        }

        // siteCd=null → COMPANY 스코프. weekStdMinutes=null → 행 삭제(주 40시간 폴백으로 복귀).
        stdWorkHoursService.saveWeekStdMinutesPolicy(
            param.cmpnyCd(), null, param.weekStdMinutes(), param.operatorUserCd());

        log.info("통상근로시간 기준값 변경 완료 - cmpnyCd={}, 주소정={}, operator={}",
            param.cmpnyCd(),
            param.weekStdMinutes() == null ? "미지정(기본 2400분)" : param.weekStdMinutes() + "분",
            param.operatorUserCd());

        return StdWorkPolicyUpdateResponse.builder()
                .cmpnyCd(param.cmpnyCd())
                .weekStdMinutes(param.weekStdMinutes())
                .policyCustomYn(param.weekStdMinutes() == null ? "N" : "Y")
                .build();
    }

    @Override
    public TokenUsageListResponse selectTokenUsageList(String cmpnyCd) {

        // 대상 검증은 한도 변경(updateTokenQuota)과 동일 기준 — 미지정/운영자 자기 자신/TB_CMPNY 미존재 거부.
        String trimmedCmpnyCd = cmpnyCd == null ? "" : cmpnyCd.trim();
        if (trimmedCmpnyCd.isEmpty()
                || PlatformConstants.PLATFORM_CMPNY_CD.equals(trimmedCmpnyCd)
                || platformCustomerMapper.selectCmpnyExists(trimmedCmpnyCd) == 0) {
            log.warn("AI 토큰 사용량 이력 조회 거부 - 대상 회사 미존재/운영자 자기 지정 - cmpnyCd={}", trimmedCmpnyCd);
            throw new ApiException(PlatformErrorCode.PLATFORM_400_015);
        }

        return TokenUsageListResponse.builder()
                .cmpnyCd(trimmedCmpnyCd)
                .usageList(platformCustomerMapper.selectTokenUsageList(trimmedCmpnyCd))
                .build();
    }
}
