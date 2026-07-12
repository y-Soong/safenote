package com.prafta.web.risk.riskai01.controller;

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
import com.prafta.web.risk.riskai01.application.param.RiskAiParam;
import com.prafta.web.risk.riskai01.dto.request.RiskAiRequest;
import com.prafta.web.risk.riskai01.dto.response.RiskAiDerivationResponse;
import com.prafta.web.risk.riskai01.service.RiskAi01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가 AI 유해요인·개선안 도출 컨트롤러(PRAFTA-WEB_003).
 *
 * <p>axios 프리픽스: /webApi/riskai01/... (ApiPrefixConfig 자동 프리픽스 — @RequestMapping 은 "/riskai01" 만).
 * <p>인증: JwtUtil.getAllClaimsAsMap. cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출(IDOR 차단),
 *    평가건 스코프 키(siteCd/processCd/assessmentCd)는 바디로 받아 서비스에서 사업장 권한·존재를 검증한다.
 */
@Slf4j
@RestController
@RequestMapping("/riskai01")
@RequiredArgsConstructor
public class RiskAi01Controller {

    private final RiskAi01Service riskAi01Service;
    private final JwtUtil jwtUtil;

    // ① 팝업 오픈 시 기존 도출 결과 + 카운터 로드
    @GetMapping("/derivation")
    public ResponseEntity<?> getDerivation(
            @ModelAttribute RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.getDerivation(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ①-2 관리자 보완 설명 저장(직접입력, blur 자동저장). body 에 suppDesc 포함(v2.1)
    @PostMapping(value = "/supplement", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveSupplement(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.saveSupplement(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ② 이미지 확정 루프 채팅(VLM). body 에 userMessage/suppDesc/kickoff/adminImages 포함(★캡 없음)
    @PostMapping(value = "/chat-image", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatImage(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.chatImage(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ②-2 이미지 이해 확정(Yes). LLM 미호출 — 이력 append + IMG_CONFIRMED='Y' 저장(v3)
    @PostMapping(value = "/confirm-image", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmImage(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.confirmImage(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ③ 유해요인·개선안 도출(RAG + LLM, ★캡 없음)
    @PostMapping(value = "/derive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> derive(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.derive(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ④ 초기화(처음부터 다시): 도출 행 DELETE(대화이력/도출결과/보완설명 전체) → 초기 상태 반환. LLM 미호출
    @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reset(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.reset(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ⑤ 미저장 정리(commit-on-save): SAVED_YN='N' 행 DELETE → 남은(확정) 행 반환. 팝업 오픈/닫기 시. LLM 미호출
    @PostMapping(value = "/discard-unsaved", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> discardUnsaved(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.discardUnsaved(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ⑥ 저장 확정(commit-on-save): 평가 저장 성공 시 SAVED_YN='Y' 확정. LLM 미호출
    @PostMapping(value = "/commit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> commit(
            @RequestBody RiskAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RiskAiDerivationResponse response = riskAi01Service.commit(
            RiskAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
