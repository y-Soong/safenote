package com.prafta.app.ai.ai01.application.param;

import java.util.List;

import com.prafta.app.ai.ai01.dto.request.AnswerRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * RAG 근거답변 Param.
 *
 * <p>RagSearchParam 과 동일한 검증 규약(식별자는 JWT 에서만, query 필수, 과대입력 방어)을 따른다.
 *    topK 는 원값(null 허용)을 보관하고, answer 전용 기본값(answer-top-k) 적용·클램프는 서비스가 수행한다.
 * <p>입력 검증:
 *   <ul>
 *     <li>tokenInfo null → COMMON_400_003 (진짜 인증 결함만 003 허용).</li>
 *     <li>query 누락/공백 → AI_400_001 (인터셉터 강제 로그아웃 회피).</li>
 *   </ul>
 */
public record AnswerParam(
    String query
    , Integer topK
    , String domainTag
    , List<String> reliabilityIn
    , List<String> trackIn
    , String gvUserCd
    , String gvCmpnyCd
) {

    /** 질의 최대 길이(과대 입력 방어 — 초과분 절단). */
    private static final int MAX_QUERY_LEN = 2000;
    /** 필터 리스트 최대 원소 수. */
    private static final int MAX_FILTER_SIZE = 20;

    public static AnswerParam from(AnswerRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String query = request.getQuery();
        if (query == null || query.isBlank())
            throw new ApiException(AiErrorCode.AI_400_001);
        query = query.trim();
        if (query.length() > MAX_QUERY_LEN)
            query = query.substring(0, MAX_QUERY_LEN);

        String domainTag = normalize(request.getDomainTag());

        return new AnswerParam(
            query
            , request.getTopK()
            , domainTag
            , capFilter(request.getReliabilityIn())
            , capFilter(request.getTrackIn())
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_cmpnyCd()
        );
    }

    private static String normalize(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static List<String> capFilter(List<String> v) {
        return (v == null || v.size() <= MAX_FILTER_SIZE) ? v : v.subList(0, MAX_FILTER_SIZE);
    }
}
