package com.prafta.common.cmm.dailylogin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.dailylogin.application.param.DailyLoginParam;
import com.prafta.common.cmm.dailylogin.dto.request.DailyLoginRequest;
import com.prafta.common.cmm.dailylogin.dto.response.DailyLoginResponse;
import com.prafta.common.cmm.dailylogin.service.DailyLoginService;
import com.prafta.common.util.ClientIpExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-app-027-2 — 일용직 직접 로그인 컨트롤러.
 *
 * <p>패키지가 com.prafta.common 이므로 ApiPrefixConfig 가 {@code /prafta/comApi/dailyLogin/...} 로 매핑한다.
 * 자격증명은 POST + JSON 본문으로만 수신한다(쿼리스트링 노출 금지). 정규 LoginController 와 완전 분리.
 */
@Slf4j
@NoAuth
@Validated
@RestController
@RequestMapping("/dailyLogin")
@RequiredArgsConstructor
public class DailyLoginController {

    private final DailyLoginService dailyLoginService;

    @NoAuth
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody DailyLoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false, defaultValue = "WEB") String clientType,
            HttpServletRequest httpRequest) {

        // prafta-com-015 4-1: 로그인 IP 는 서버가 추출(디바이스 로그인 이력 LOGIN_IP). 정규 LoginController 패턴 동일.
        String ipAddr = ClientIpExtractor.extract(httpRequest);

        DailyLoginResponse response = dailyLoginService.login(DailyLoginParam.from(request, clientType, ipAddr));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
