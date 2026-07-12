package com.prafta.app.ai.ai01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.ai.ai01.application.param.AnswerParam;
import com.prafta.app.ai.ai01.application.param.RagSearchParam;
import com.prafta.app.ai.ai01.dto.request.AnswerRequest;
import com.prafta.app.ai.ai01.dto.request.RagSearchRequest;
import com.prafta.app.ai.ai01.dto.response.AnswerResponse;
import com.prafta.app.ai.ai01.dto.response.RagSearchResponse;
import com.prafta.app.ai.ai01.service.Ai01Service;
import com.prafta.app.ai.ai01.service.AnswerService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG(AI 검색) 컨트롤러 (앱).
 *
 * <p>base URL: ApiPrefixConfig 자동 프리픽스(com.prafta.app.*) → {@code /prafta/appApi/ai01/*}.
 *    (프리픽스 수동 추가 금지 — @RequestMapping 은 "/ai01" 만 둔다.)
 * <p>인증: 기존 JWT 필터 + DailyUserActiveGate 상속. 사내 사용자 조회는 광범위 허용(정책 §6)이라
 *    별도 역할 게이트는 두지 않는다. 식별자는 JWT 클레임에서만 도출(IDOR 무관, 조회 전용).
 */
@Slf4j
@RestController
@RequestMapping("/ai01")
@RequiredArgsConstructor
public class Ai01Controller {

    private final Ai01Service ai01Service;
    private final AnswerService answerService;
    private final JwtUtil jwtUtil;

    // ① 검색: 질의 임베딩 → pgvector top-K → 출처/신뢰등급/트랙 포함 반환(LLM 없음).
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(
            @RequestBody RagSearchRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        RagSearchResponse response = ai01Service.search(RagSearchParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ② 근거답변(Phase 2): 검색 재사용 → track 분리 → recompose 만 LLM 합성 → verbatim 원문 첨부.
    //    검색과 동일한 JWT/토큰 패턴. 게이트 OFF 면 AI_503_001.
    @PostMapping(value = "/answer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> answer(
            @RequestBody AnswerRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AnswerResponse response = answerService.answer(AnswerParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
