package com.prafta.app.device.device01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.device.device01.application.param.PushTokenParam;
import com.prafta.app.device.device01.dto.request.PushTokenRequest;
import com.prafta.app.device.device01.dto.response.PushTokenResponse;
import com.prafta.app.device.device01.service.Device01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 단말 푸시 토큰 등록 (앱) 컨트롤러.
 *
 * <p>USER_CD 는 JWT 클레임(gv_userCd)에서만 도출하여 IDOR 을 차단한다.
 *    DEVICE_UUID 는 본문 도착값(gv_deviceId, axios 인터셉터 자동 동봉)을 사용한다.
 *    base URL: ApiPrefixConfig 자동 프리픽스 → /appApi/device01/...
 * <p>앱 완전분리 원칙: web 컨트롤러를 호출하지 않고 tb_user_device 만 공유한다.
 */
@Slf4j
@RestController
@RequestMapping("/device01")
@RequiredArgsConstructor
public class Device01Controller {

    private final Device01Service device01Service;
    private final JwtUtil jwtUtil;

    // F01 푸시 토큰 등록 (로그인 직후/refresh 시 호출, DEVICE_UUID 기준 upsert)
    @PostMapping(value = "/push-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> upsertPushToken(
            @RequestBody PushTokenRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        PushTokenResponse response = device01Service.upsertPushToken(
            PushTokenParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
