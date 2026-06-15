package com.prafta.web.risk.risklink01.controller;

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
import com.prafta.web.risk.risklink01.application.param.AvailableNearMissParam;
import com.prafta.web.risk.risklink01.application.param.LinkedNearMissParam;
import com.prafta.web.risk.risklink01.application.param.NearMissLinkParam;
import com.prafta.web.risk.risklink01.dto.request.AvailableNearMissRequest;
import com.prafta.web.risk.risklink01.dto.request.LinkedNearMissRequest;
import com.prafta.web.risk.risklink01.dto.request.NearMissLinkRequest;
import com.prafta.web.risk.risklink01.dto.response.NearMissLinkListResponse;
import com.prafta.web.risk.risklink01.service.RiskLink01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가-아차사고 참조 연계(prafta-054-3) 컨트롤러.
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 * axios 프리픽스: /webApi/risklink01/...
 */
@Slf4j
@RestController
@RequestMapping("/risklink01")
@RequiredArgsConstructor
public class RiskLink01Controller {

    private final RiskLink01Service riskLink01Service;
    private final JwtUtil jwtUtil;

    // L1 연결 후보 아차사고 검색 (완료400 + 같은 사업장 + 미연결 + 검색어)
    @GetMapping("/available-near-miss")
    public ResponseEntity<?> getAvailableNearMiss(
            @ModelAttribute AvailableNearMissRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NearMissLinkListResponse response = riskLink01Service.selectAvailableNearMiss(
            AvailableNearMissParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // L2 연결된 아차사고 목록 (USE_YN='Y')
    @GetMapping("/linked-near-miss")
    public ResponseEntity<?> getLinkedNearMiss(
            @ModelAttribute LinkedNearMissRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NearMissLinkListResponse response = riskLink01Service.selectLinkedNearMiss(
            LinkedNearMissParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // L3 연결 추가 (개선완료003 전 + 완료400 아차사고 검증 + upsert)
    @PostMapping(value = "/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> link(
            @RequestBody NearMissLinkRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        riskLink01Service.linkNearMiss(
            NearMissLinkParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // L4 연결 해제 (개선완료003 전 + soft delete USE_YN='N')
    @PostMapping(value = "/unlink", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlink(
            @RequestBody NearMissLinkRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        riskLink01Service.unlinkNearMiss(
            NearMissLinkParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
