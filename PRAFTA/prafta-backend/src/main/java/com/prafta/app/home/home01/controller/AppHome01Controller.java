package com.prafta.app.home.home01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.home.home01.application.param.HomeSummaryParam;
import com.prafta.app.home.home01.dto.response.HomeSummaryResponse;
import com.prafta.app.home.home01.service.AppHome01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-001: 앱 메인화면 요약 컨트롤러.
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 *   GET /prafta/appApi/home01/home-summary
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는
 *   jwtUtil.getAllClaimsAsMap(Authorization) → TokenInfo 로 cmpnyCd/siteCd/userCd 를 도출한다.
 *   userCd 등 식별값을 쿼리/바디로 받지 않는다. (AppChkLst01Controller 패턴 동일)
 */
@Slf4j
@RestController
@RequestMapping("/home01")
@RequiredArgsConstructor
public class AppHome01Controller {

    private final AppHome01Service appHome01Service;
    private final JwtUtil jwtUtil;

    /**
     * 메인화면 요약 조회(attendance / leave / approval / tbm 단일 응답).
     */
    @GetMapping("/home-summary")
    public ResponseEntity<?> getHomeSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        HomeSummaryResponse response = appHome01Service.selectHomeSummary(
                HomeSummaryParam.from(tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
