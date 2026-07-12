package com.prafta.app.user.user01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.user.user01.dto.request.AppTransferNoticeAckRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user01.dto.response.TransferNoticeResponse;
import com.prafta.web.user.user01.service.User01TransferService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 사용자 소속이동 안내 컨트롤러 — PRAFTA-WEB_001-5(Terminal E).
 *
 * <p>자동 프리픽스(com.prafta.app.* → /prafta/appApi) 적용 → 실제 매핑:
 *   <ul>
 *     <li>GET  /prafta/appApi/user01/my-transfer-notice — 로그인 안내(미확인 소속이동 예약 1건)</li>
 *     <li>POST /prafta/appApi/user01/transfer-notice/ack — 안내 확인(advisory, best-effort)</li>
 *   </ul>
 *
 * <p>로직 신규 0: Terminal C 의 {@link User01TransferService} 를 그대로 재사용(주입)한다.
 * 서비스 시그니처가 (cmpnyCd, userCd) 범용이라 웹(webApi)/앱(appApi) 컨트롤러가 동일 서비스를 공용한다.
 *
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는 jwtUtil.getAllClaimsAsMap(Authorization)
 *    → TokenInfo 로 cmpnyCd/userCd 를 도출한다(IDOR 차단 — 클라가 보낸 식별자는 신뢰하지 않는다).
 */
@Slf4j
@RestController
@RequestMapping("/user01")
@RequiredArgsConstructor
public class AppUser01Controller {

    private final User01TransferService user01TransferService;
    private final JwtUtil jwtUtil;

    /**
     * 로그인 안내 — 본인의 미확인 소속이동 예약 1건 조회. 대상은 토큰에서만 도출(IDOR 방지).
     * 미확인 예약이 없으면 hasNotice=false.
     */
    @GetMapping("/my-transfer-notice")
    public ResponseEntity<?> getMyTransferNotice(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        TransferNoticeResponse response =
                user01TransferService.getMyTransferNotice(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 안내 확인(ack) — 본인 소유 예약만 확인 처리(advisory). 대상은 토큰, 확인할 예약 ID 는 바디.
     * 타인/미존재 예약은 서비스가 404 처리(존재 비노출).
     */
    @PostMapping(value = "/transfer-notice/ack", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ackTransferNotice(
            @RequestBody AppTransferNoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        user01TransferService.ackTransferNotice(
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_userCd(),
                request == null ? null : request.getReservationId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함(COMMON_400_003). */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        return tokenInfo;
    }
}
