package com.prafta.platform.customer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.application.param.StdWorkPolicyUpdateParam;
import com.prafta.platform.customer.application.param.TokenQuotaUpdateParam;
import com.prafta.platform.customer.dto.request.CustomerListRequest;
import com.prafta.platform.customer.dto.request.StdWorkPolicyUpdateRequest;
import com.prafta.platform.customer.dto.request.TokenQuotaUpdateRequest;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.dto.response.StdWorkPolicyUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenQuotaUpdateResponse;
import com.prafta.platform.customer.dto.response.TokenUsageListResponse;
import com.prafta.platform.customer.service.PlatformCustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 플랫폼 고객 리스트 컨트롤러(Platform_03 — 플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code GET /prafta/platformApi/customer/customer-lists}
 * / {@code POST /prafta/platformApi/customer/token-quota}
 * / {@code POST /prafta/platformApi/customer/std-work-policy}
 * (패키지 {@code com.prafta.platform.*} → ApiPrefixConfig 가 {@code /prafta/platformApi} 프리픽스 부여).
 *
 * <p>운영자 인가/IP 게이트는 {@code PlatformOperatorGateInterceptor} 가, JWT 유효성은 {@code AuthAspect}
 * 가 강제한다. Platform_04(위치정보 열람)의 회사 select 도 본 API 를 재사용한다.
 */
@Slf4j
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class PlatformCustomerController {

    private final PlatformCustomerService platformCustomerService;
    private final JwtUtil jwtUtil;

    @GetMapping("/customer-lists")
    public ResponseEntity<?> getCustomerList(@ModelAttribute CustomerListRequest request) {

        CustomerListResponse response = platformCustomerService.selectCustomerList(
                CustomerListParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 회사별 월간 AI 토큰 사용량 이력 조회(최근 24개월 — Platform_03 이력 팝업, read-only). */
    @GetMapping("/token-usage-lists")
    public ResponseEntity<?> getTokenUsageList(@RequestParam(value = "cmpnyCd", required = false) String cmpnyCd) {

        TokenUsageListResponse response = platformCustomerService.selectTokenUsageList(cmpnyCd);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 회사별 월간 AI 토큰 한도 변경(플랫폼-AI-토큰쿼터 §2-2/8 — 운영자 식별은 토큰에서만). */
    @PostMapping("/token-quota")
    public ResponseEntity<?> updateTokenQuota(
            @RequestBody TokenQuotaUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenQuotaUpdateResponse response = platformCustomerService.updateTokenQuota(
                TokenQuotaUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 회사 통상근로자 주 소정근로 기준값 변경(운영자 식별은 토큰에서만).
     *
     * <p>회사 등록(Platform_01)에서만 입력 가능하던 기준값의 사후 정정 경로다.
     * 단시간 판정 분모·연차 비례부여 분모가 바뀌므로 운영자 전용 콘솔에서만 노출한다.
     */
    @PostMapping("/std-work-policy")
    public ResponseEntity<?> updateStdWorkPolicy(
            @RequestBody StdWorkPolicyUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        StdWorkPolicyUpdateResponse response = platformCustomerService.updateStdWorkPolicy(
                StdWorkPolicyUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
