package com.prafta.web.tbm.tbmai02.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.tbm.tbmai02.application.param.TbmAi02Param;
import com.prafta.web.tbm.tbmai02.dto.request.TbmAi02Request;
import com.prafta.web.tbm.tbmai02.dto.response.TbmAiUnconfirmedResponse;
import com.prafta.web.tbm.tbmai02.dto.response.TbmGenerateResponse;
import com.prafta.web.tbm.tbmai02.service.TbmAi02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM AI 교육안 생성 컨트롤러(TBM_AI v4 RC).
 *
 * <p>axios 프리픽스: /webApi/tbmai02/... (ApiPrefixConfig 자동 프리픽스 — @RequestMapping 은 "/tbmai02" 만).
 * <p>인증: JwtUtil.getAllClaimsAsMap. cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출(IDOR 차단),
 *    세션 식별자(sessionCd)는 바디로 받아 서비스에서 회사 소유·존재를 검증한다.
 */
@Slf4j
@RestController
@RequestMapping("/tbmai02")
@RequiredArgsConstructor
public class TbmAi02Controller {

    private final TbmAi02Service tbmAi02Service;
    private final JwtUtil jwtUtil;

    // 교육안 생성(세션에 묶인 CONFIRMED 확정 서술 + 관리자 교육내용 → 라인프로토콜 4섹션 리치HTML 초안 반환, DB 미기록)
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generate(
            @RequestBody TbmAi02Request request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TbmGenerateResponse response = tbmAi02Service.generate(
            TbmAi02Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 미확정 AI 분석 항목 조회(FE 사전 차단용, 조회 전용 — 세션 상태/LLM 게이트 미검사, DB/감사 미기록)
    @GetMapping("/unconfirmed-items")
    public ResponseEntity<?> unconfirmedItems(
            @ModelAttribute TbmAi02Request request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TbmAiUnconfirmedResponse response = tbmAi02Service.unconfirmedItems(
            TbmAi02Param.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
