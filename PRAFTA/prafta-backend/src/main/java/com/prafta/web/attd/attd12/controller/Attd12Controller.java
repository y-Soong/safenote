package com.prafta.web.attd.attd12.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd12.application.param.FraudAttdSuspectParam;
import com.prafta.web.attd.attd12.dto.request.FraudAttdSuspectRequest;
import com.prafta.web.attd.attd12.dto.response.FraudAttdSuspectResponse;
import com.prafta.web.attd.attd12.service.Attd12Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-003 C6 - Attd_12 부정 출퇴근 의심 모니터링 컨트롤러.
 * 읽기 전용 조회 API(표시 전용, 차단 없음 — D4). Attd_11 GET 패턴 동일.
 */
@Slf4j
@RestController
@RequestMapping("/attd12")
@RequiredArgsConstructor
public class Attd12Controller {

    private final Attd12Service attd12Service;
    private final JwtUtil jwtUtil;

    /** 부정 출퇴근 의심 케이스 조회. (프론트: /webApi/attd12/fraud-attd-suspects) */
    @GetMapping("/fraud-attd-suspects")
    public ResponseEntity<?> getFraudAttdSuspects(
            @ModelAttribute FraudAttdSuspectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        FraudAttdSuspectResponse response = attd12Service.getFraudAttdSuspects(
                FraudAttdSuspectParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
