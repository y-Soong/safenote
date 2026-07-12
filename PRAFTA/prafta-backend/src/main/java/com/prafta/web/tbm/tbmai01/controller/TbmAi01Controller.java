package com.prafta.web.tbm.tbmai01.controller;

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
import com.prafta.web.tbm.tbmai01.application.param.TbmAiParam;
import com.prafta.web.tbm.tbmai01.application.param.TbmAiWorklistParam;
import com.prafta.web.tbm.tbmai01.dto.request.TbmAiRequest;
import com.prafta.web.tbm.tbmai01.dto.request.TbmAiWorklistRequest;
import com.prafta.web.tbm.tbmai01.service.TbmAi01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 교육자료 세부항목 AI 분석·확정 컨트롤러(TBM_AI T1).
 *
 * <p>axios 프리픽스: /webApi/tbmai01/... (ApiPrefixConfig 자동 프리픽스 — @RequestMapping 은 "/tbmai01" 만).
 * <p>인증: JwtUtil.getAllClaimsAsMap. cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출(IDOR 차단),
 *    자료/항목 스코프 키(mtrlCd/mtrlItemCd)는 바디로 받아 서비스에서 회사 스코프 조인으로 검증한다.
 */
@Slf4j
@RestController
@RequestMapping("/tbmai01")
@RequiredArgsConstructor
public class TbmAi01Controller {

    private final TbmAi01Service tbmAi01Service;
    private final JwtUtil jwtUtil;

    // ① 초기 큐잉: 자료의 분석지정 이미지·PDF 항목 ANALYZING + 비동기 분석 트리거
    @PostMapping(value = "/analyze-items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyzeItems(
            @RequestBody TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.analyzeItems(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ② 분석 상태 조회(폴링, 읽기전용)
    @GetMapping("/analysis-status")
    public ResponseEntity<?> analysisStatus(
            @ModelAttribute TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.analysisStatus(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ③ 대화형 보완(VLM, 이미지 매요청 재부착)
    @PostMapping(value = "/chat-item", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatItem(
            @RequestBody TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.chatItem(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ④ 확정(LLM 미호출, 멱등)
    @PostMapping(value = "/confirm-item", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmItem(
            @RequestBody TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.confirmItem(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ⑤ 재분석 재진입(ANALYZING + 비동기 트리거)
    @PostMapping(value = "/reanalyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reanalyze(
            @RequestBody TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.reanalyze(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ⑥ 실패 항목 수기 확정(LLM 미호출)
    @PostMapping(value = "/manual-confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> manualConfirm(
            @RequestBody TbmAiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.manualConfirm(TbmAiParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ⑦ 분석 워크리스트 조회(회사/사업장 스코프 + 검색/필터/페이징, 읽기전용)
    @GetMapping("/analysis-worklist")
    public ResponseEntity<?> analysisWorklist(
            @ModelAttribute TbmAiWorklistRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            tbmAi01Service.worklist(TbmAiWorklistParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }
}
