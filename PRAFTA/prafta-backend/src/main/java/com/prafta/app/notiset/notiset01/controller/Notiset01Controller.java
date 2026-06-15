package com.prafta.app.notiset.notiset01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.notiset.notiset01.application.param.PushSettingQueryParam;
import com.prafta.app.notiset.notiset01.application.param.PushSettingSaveParam;
import com.prafta.app.notiset.notiset01.dto.request.PushSettingSaveRequest;
import com.prafta.app.notiset.notiset01.dto.response.PushSettingResponse;
import com.prafta.app.notiset.notiset01.service.Notiset01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 푸시 알림 수신 설정 (앱) 컨트롤러 (PRAFTA-APP-021-1).
 *
 * <p>USER_CD/CMPNY_CD/AUTH_CD 는 JWT 클레임에서만 도출하여 IDOR 을 차단한다(본문 식별자 미사용).
 *    base URL: ApiPrefixConfig 자동 프리픽스 → /appApi/notiset01/...
 * <p>앱 완전분리 원칙: web 컨트롤러를 호출하지 않고 자체 도메인으로 설정만 조회/저장한다.
 */
@Slf4j
@RestController
@RequestMapping("/notiset01")
@RequiredArgsConstructor
public class Notiset01Controller {

    private final Notiset01Service notiset01Service;
    private final JwtUtil jwtUtil;

    // 본인 푸시 설정 + isAdmin 조회(미설정=ON 보정).
    @GetMapping(value = "/settings")
    public ResponseEntity<?> getSettings(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        PushSettingResponse response =
                notiset01Service.getMySettings(PushSettingQueryParam.from(tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 본인 푸시 설정 저장(마스터 + 토글 일괄 upsert). 갱신 후 최신 설정 반환.
    @PutMapping(value = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveSettings(
            @RequestBody PushSettingSaveRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        PushSettingResponse response =
                notiset01Service.saveMySettings(PushSettingSaveParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
