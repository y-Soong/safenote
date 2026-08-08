package com.prafta.platform.location.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.sms.policy.SmsClientIpResolver;
import com.prafta.common.security.JwtUtil;
import com.prafta.platform.common.PlatformOperatorGateInterceptor;
import com.prafta.platform.location.application.param.GpsListParam;
import com.prafta.platform.location.application.param.LocationSiteListParam;
import com.prafta.platform.location.application.param.PlatformOperatorParam;
import com.prafta.platform.location.application.param.SmsVerifyParam;
import com.prafta.platform.location.dto.request.GpsListRequest;
import com.prafta.platform.location.dto.request.LocationSiteListRequest;
import com.prafta.platform.location.dto.request.SmsVerifyRequest;
import com.prafta.platform.location.dto.response.GpsListResponse;
import com.prafta.platform.location.dto.response.LocationSiteListResponse;
import com.prafta.platform.location.dto.response.SmsStatusResponse;
import com.prafta.platform.location.service.PlatformLocationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 플랫폼 위치정보 열람 컨트롤러(Platform_04 — 플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code /prafta/platformApi/location/*}
 * (패키지 {@code com.prafta.platform.*} → ApiPrefixConfig 가 {@code /prafta/platformApi} 프리픽스 부여).
 *
 * <p>운영자 인가/IP 게이트는 {@code PlatformOperatorGateInterceptor} 가 강제하고,
 * 위치정보 조회(gps-lists)는 추가로 SMS 인증 게이트(서버 판정, 10분 유효)를 통과해야 한다
 * (LBS 법적 요건 — 요청서 §5-1). 조회 1회당 열람 로그 1건이 동일 트랜잭션으로 기록된다(§5-2).
 */
@Slf4j
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class PlatformLocationController {

    private final PlatformLocationService platformLocationService;
    private final JwtUtil jwtUtil;
    /** 클라이언트 IP 해석 규칙 단일 출처(신뢰 프록시 검증 포함 — 게이트와 동일 규칙으로 열람 로그 기록). */
    private final PlatformOperatorGateInterceptor platformOperatorGateInterceptor;
    /** SMS2-B2/B4: SMS 상한 IP 축 <b>전용</b> 해석기(우측 홉 채택). 열람 로그 IP 와 용도가 다르다. */
    private final SmsClientIpResolver smsClientIpResolver;

    /**
     * SMS 인증코드 발송 — 클라 휴대폰 입력 없음(토큰의 운영자 본인 등록 휴대폰). 응답에도 휴대폰값 미포함.
     *
     * <p>SMS2-B4: IP 축 상한 재료(해시)를 여기서 해석해 Param 으로 넘긴다.
     * ★{@code SmsClientIpResolver} 는 SMS 상한 전용이며, 열람 로그의 IP 기록은 계속
     * {@code PlatformOperatorGateInterceptor.resolveClientIp()} 가 단일 출처다(규칙 분기 금지).
     */
    @PostMapping("/sms-send")
    public ResponseEntity<?> sendSmsAuth(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpServletRequest) {

        platformLocationService.sendSmsAuth(
                PlatformOperatorParam.from(jwtUtil.getAllClaimsAsMap(authorization)
                        , smsClientIpResolver.resolveIpHash(httpServletRequest)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** SMS 인증번호 검증 — 통과 시 10분간 gps-lists 조회 허용(서버 판정). */
    @PostMapping("/sms-verify")
    public ResponseEntity<?> verifySmsAuth(
            @RequestBody SmsVerifyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        platformLocationService.verifySmsAuth(
                SmsVerifyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 현재 SMS 인증 상태(verified/remainSec) — 화면 재진입 시 불필요한 재인증 방지. */
    @GetMapping("/sms-status")
    public ResponseEntity<?> getSmsStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SmsStatusResponse response = platformLocationService.selectSmsStatus(
                PlatformOperatorParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 대상 회사 사업장 목록(좌표/지오펜스 반경 — 지도 중심용, 시설 좌표라 SMS 게이트 미적용). */
    @GetMapping("/site-lists")
    public ResponseEntity<?> getSiteList(@ModelAttribute LocationSiteListRequest request) {

        LocationSiteListResponse response = platformLocationService.selectSiteList(
                LocationSiteListParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 위치정보 조회(근태 GPS + TBM 입실 UNION, LIMIT 1000 + truncated).
     *
     * <p>매 호출: ① SMS 인증 서버 판정(만료 시 403 PLATFORM_403_003) → ② 조회 → ③ 열람 로그
     * INSERT(동일 트랜잭션). CLIENT_IP 는 게이트 인터셉터와 동일 규칙(신뢰 프록시 경유 시에만
     * X-Forwarded-For 선두 채택)으로 기록하고, REMOTE_ADDR(직접 연결 IP 원시값)를 병기한다(V-1).
     */
    @GetMapping("/gps-lists")
    public ResponseEntity<?> getGpsList(
            @ModelAttribute GpsListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpServletRequest) {

        GpsListResponse response = platformLocationService.selectGpsList(GpsListParam.from(
                request
                , jwtUtil.getAllClaimsAsMap(authorization)
                , platformOperatorGateInterceptor.resolveClientIp(httpServletRequest)
                , httpServletRequest.getRemoteAddr()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
