package com.prafta.platform.company.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.platform.company.application.param.CompanyProvisionParam;
import com.prafta.platform.company.dto.request.CompanyProvisionRequest;
import com.prafta.platform.company.dto.response.CompanyProvisionResponse;
import com.prafta.platform.company.service.CompanyProvisionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 신규 고객사 프로비저닝 컨트롤러(플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code POST /prafta/platformApi/company}
 * (패키지 {@code com.prafta.platform.*} → ApiPrefixConfig 가 {@code /prafta/platformApi} 프리픽스 부여).
 *
 * <p>운영자 인가/IP 게이트는 {@code PlatformOperatorGateInterceptor} 가, JWT 유효성은 {@code AuthAspect}
 * 가 강제하므로 본 컨트롤러는 토큰에서 운영자 식별자만 도출해 서비스에 전달한다.
 */
@Slf4j
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyProvisionController {

    private final CompanyProvisionService companyProvisionService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> provisionCompany(
            @RequestBody CompanyProvisionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        CompanyProvisionResponse response = companyProvisionService.provisionCompany(
                CompanyProvisionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
