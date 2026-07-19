package com.prafta.common.cmm.aiquota.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.aiquota.application.result.AiQuotaStatusResult;

/**
 * 회사별 월간 AI 토큰 쿼터 매퍼(TB_AI_TOKEN_QUOTA / TB_AI_TOKEN_USAGE).
 */
@Mapper
public interface AiQuotaMapper {

    /** 유효한도·당월 사용량 1회 조회(스칼라 서브쿼리 — 항상 1행 반환). */
    AiQuotaStatusResult selectQuotaStatus(@Param("cmpnyCd") String cmpnyCd, @Param("useYm") String useYm);

    /** 사용량 누적 UPSERT(월 키 신규 행 자동 생성 + 기존 행 증가). */
    int upsertUsage(@Param("cmpnyCd") String cmpnyCd
        , @Param("useYm") String useYm
        , @Param("inputTokens") long inputTokens
        , @Param("outputTokens") long outputTokens);
}
