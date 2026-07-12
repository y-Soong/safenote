package com.prafta.app.ai.ai01.application.param;

import java.util.List;

import com.prafta.app.ai.ai01.dto.request.RagSearchRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * RAG 검색 Param.
 *
 * <p>식별자(userCd/cmpnyCd)는 JWT 클레임에서만 도출한다(감사 로깅 용도). query 는 필수.
 *    topK 는 원값(null 허용)을 그대로 보관하고, 클램프(기본/상한)는 서비스가 설정값으로 수행한다.
 * <p>입력 검증:
 *   <ul>
 *     <li>tokenInfo null → COMMON_400_003 (진짜 인증 결함만 003 허용).</li>
 *     <li>query 누락/공백 → AI_400_001 (인터셉터 강제 로그아웃 회피).</li>
 *   </ul>
 */
public record RagSearchParam(
    String query
    , Integer topK
    , String domainTag
    , List<String> reliabilityIn
    , List<String> trackIn
    , String gvUserCd
    , String gvCmpnyCd
) {

    /** 질의 최대 길이(과대 입력 방어 — 초과분은 절단). BGE-m3 상한(토큰)과 무관한 페이로드 방어. */
    private static final int MAX_QUERY_LEN = 2000;
    /** 필터 리스트 최대 원소 수(신뢰등급·트랙은 소수 열거값 — 과대 리스트 방어). */
    private static final int MAX_FILTER_SIZE = 20;

    public static RagSearchParam from(RagSearchRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String query = request.getQuery();
        if (query == null || query.isBlank())
            throw new ApiException(AiErrorCode.AI_400_001);
        query = query.trim();
        if (query.length() > MAX_QUERY_LEN)     // 과대 질의 절단(DoS 방어)
            query = query.substring(0, MAX_QUERY_LEN);

        String domainTag = normalize(request.getDomainTag());

        return new RagSearchParam(
            query
            , request.getTopK()
            , domainTag
            , capFilter(request.getReliabilityIn())
            , capFilter(request.getTrackIn())
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_cmpnyCd()
        );
    }

    /** 공백-only 문자열은 null(필터 미적용)로 정규화. */
    private static String normalize(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 필터 리스트 크기 상한(과대 입력 방어). null 유지, 상한 초과 시 앞부분만. */
    private static List<String> capFilter(List<String> v) {
        return (v == null || v.size() <= MAX_FILTER_SIZE) ? v : v.subList(0, MAX_FILTER_SIZE);
    }
}
