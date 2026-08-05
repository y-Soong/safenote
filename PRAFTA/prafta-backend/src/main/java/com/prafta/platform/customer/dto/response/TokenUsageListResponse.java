package com.prafta.platform.customer.dto.response;

import java.util.List;

import com.prafta.platform.customer.application.result.TokenUsageListResult;

import lombok.Builder;
import lombok.Value;

/**
 * 회사별 월간 AI 토큰 사용량 이력 조회 응답 DTO(Platform_03 이력 팝업).
 *
 * <p>이력은 최근 24개월 한정(USE_YM 내림차순). 사용량 축적은 쿼터 기능 도입월부터라
 * 그 이전 연월 행은 존재하지 않는다(0 채움 없이 실존 행만 응답).
 */
@Value
@Builder
public class TokenUsageListResponse {

    /** 대상 회사코드(요청 에코 — 팝업 표시 검증용). */
    String cmpnyCd;

    /** 월별 사용량 목록(USE_YM 내림차순, 최근 24개월). */
    List<TokenUsageListResult> usageList;
}
