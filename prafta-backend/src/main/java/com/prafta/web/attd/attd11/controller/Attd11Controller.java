package com.prafta.web.attd.attd11.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd11.application.param.MonthlyAttdSummaryParam;
import com.prafta.web.attd.attd11.dto.request.MonthlyAttdSummaryRequest;
import com.prafta.web.attd.attd11.dto.response.MonthlyAttdSummaryResponse;
import com.prafta.web.attd.attd11.service.Attd11Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 컨트롤러.
 * 읽기 전용 조회 API. attd07 컨트롤러 GET 패턴 동일.
 */
@Slf4j
@RestController
@RequestMapping("/attd11")
@RequiredArgsConstructor
public class Attd11Controller {

    private final Attd11Service attd11Service;
    private final JwtUtil jwtUtil;

    /** 월별 사용자 근태 판정 종합 조회. (프론트: /webApi/attd11/monthly-attd-summary) */
    @GetMapping("/monthly-attd-summary")
    public ResponseEntity<?> getMonthlyAttdSummary(
            @ModelAttribute MonthlyAttdSummaryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        MonthlyAttdSummaryResponse response = attd11Service.getMonthlyAttdSummary(
                MonthlyAttdSummaryParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
