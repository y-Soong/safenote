package com.prafta.app.auth.auth01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.auth.auth01.application.param.WithdrawParam;
import com.prafta.app.auth.auth01.dto.request.WithdrawRequest;
import com.prafta.app.auth.auth01.service.AppAuth01Service;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-010-07: 모바일 앱 회원 탈퇴 컨트롤러 (auth01).
 *
 * <p>최종 URL: /prafta/appApi/auth/withdraw. CMPNY_CD/USER_CD 는 JWT 에서만 도출(IDOR 차단).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AppAuth01Controller {

    private final AppAuth01Service appAuth01Service;
    private final JwtUtil jwtUtil;

    /** 010-07: 회원 탈퇴(D5: 연차 자동취소·결재자 알림 제외). */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appAuth01Service.withdraw(
                WithdrawParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }
}
