package com.prafta.platform.customer.service;

import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.application.param.StdWorkPolicyUpdateParam;
import com.prafta.platform.customer.application.param.TokenQuotaUpdateParam;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.dto.response.StdWorkPolicyUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenQuotaUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenUsageListResponse;

/**
 * 플랫폼 고객 리스트 서비스(Platform_03).
 *
 * <p>목록 조회는 read-only, AI 토큰 한도 변경(token-quota)은 TB_AI_TOKEN_QUOTA UPSERT 쓰기,
 * 통상근로시간 기준값 변경(std-work-policy)은 TB_CMPNY_STD_WORK_POLICY UPSERT/DELETE 쓰기.
 */
public interface PlatformCustomerService {

    /** 고객사 목록 조회(검색: 회사명 부분일치/계약여부/사용여부, LIMIT 500 + 전체 건수 + 당월 AI 사용량/한도). */
    CustomerListResponse selectCustomerList(CustomerListParam param);

    /** 회사별 월간 AI 토큰 한도 UPSERT(플랫폼-AI-토큰쿼터 §2-2/8). */
    TokenQuotaUpdateResponse updateTokenQuota(TokenQuotaUpdateParam param);

    /** 회사별 월간 AI 토큰 사용량 이력 조회(최근 24개월, read-only — Platform_03 이력 팝업). */
    TokenUsageListResponse selectTokenUsageList(String cmpnyCd);

    /**
     * 회사 통상근로자 주 소정근로 기준값 변경(TB_CMPNY_STD_WORK_POLICY COMPANY 스코프).
     *
     * <p>회사 등록(Platform_01)에서만 입력할 수 있던 기준값의 <b>사후 정정 경로</b>다.
     * {@code param.weekStdMinutes()} 가 null 이면 지정을 해제(행 삭제)해 주 40시간 폴백으로 되돌린다.
     */
    StdWorkPolicyUpdateResponse updateStdWorkPolicy(StdWorkPolicyUpdateParam param);
}
