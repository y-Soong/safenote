package com.prafta.platform.customer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.platform.customer.application.query.CustomerListQuery;
import com.prafta.platform.customer.application.result.CustomerListResult;

/**
 * 플랫폼 고객 리스트 매퍼.
 *
 * <p>TB_CMPNY 는 SELECT 전용(고객사 마스터 쓰기 금지 유지). 쓰기 statement 는
 * AI 토큰 한도(TB_AI_TOKEN_QUOTA) UPSERT 1건만 허용한다(플랫폼-AI-토큰쿼터 T4).
 */
@Mapper
public interface PlatformCustomerMapper {

    /** 고객사 목록 조회(LIMIT 500 고정, 운영자 자기 자신 제외, 당월 AI 사용량/한도 LEFT JOIN). */
    List<CustomerListResult> selectCustomerList(CustomerListQuery query);

    /** 검색 조건 일치 전체 건수(LIMIT 무관 — 절단 판정용). */
    int selectCustomerListCount(CustomerListQuery query);

    /** 대상 회사 존재 여부(TB_CMPNY COUNT — 한도 수정 대상 검증용). */
    int selectCmpnyExists(@Param("cmpnyCd") String cmpnyCd);

    /** 회사별 월간 AI 토큰 한도 UPSERT(감사컬럼에 운영자 userCd 기록). */
    int upsertTokenQuota(@Param("cmpnyCd") String cmpnyCd
        , @Param("monthlyTokenLimit") long monthlyTokenLimit
        , @Param("operatorUserCd") String operatorUserCd);
}
