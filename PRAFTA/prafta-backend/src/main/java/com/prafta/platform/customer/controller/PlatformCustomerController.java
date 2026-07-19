package com.prafta.platform.customer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.platform.customer.application.param.CustomerListParam;
import com.prafta.platform.customer.dto.request.CustomerListRequest;
import com.prafta.platform.customer.dto.response.CustomerListResponse;
import com.prafta.platform.customer.service.PlatformCustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 플랫폼 고객 리스트 컨트롤러(Platform_03 — 플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code GET /prafta/platformApi/customer/customer-lists}
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

    @GetMapping("/customer-lists")
    public ResponseEntity<?> getCustomerList(@ModelAttribute CustomerListRequest request) {

        CustomerListResponse response = platformCustomerService.selectCustomerList(
                CustomerListParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
