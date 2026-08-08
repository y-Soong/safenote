package com.prafta.platform.sms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.platform.common.PlatformOperatorGateInterceptor;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.dto.request.SmsPolicyUpdateRequest;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;
import com.prafta.platform.sms.service.PlatformSmsService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMS 발송 관리 컨트롤러(Platform_05 — 플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code /prafta/platformApi/sms/*}
 * (패키지 {@code com.prafta.platform.*} → {@code ApiPrefixConfig} 가 {@code /prafta/platformApi} 프리픽스 부여).
 *
 * <p><b>★인가</b>: {@code PlatformOperatorGateInterceptor} 가 {@code /prafta/platformApi/**} 전체에 등록돼
 * JWT {@code gv_cmpnyCd == prafta_system_admin} 을 강제한다(아니면 {@code PLATFORM_403_001}).
 * 컨트롤러에 별도 어노테이션이 필요 없으며, 메뉴 숨김은 발견 방지(UX)일 뿐 실제 차단은 이 게이트다.
 * <p>★참고: IP 허용목록 계층({@code prafta.platform.allowed-ips})은 {@code application-local.properties}
 * 에만 있어 <b>운영에서는 현재 비활성</b>이다. "IP 로도 막히니 괜찮다" 고 오판하지 말 것(별건 백로그).
 *
 * <p>★운영자 식별은 <b>토큰에서만</b> 한다(요청 바디의 사용자 값 불신 — IDOR 방지,
 * {@code PlatformCustomerController.updateTokenQuota} 선례).
 */
@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class PlatformSmsController {

    private final PlatformSmsService platformSmsService;
    private final JwtUtil jwtUtil;

    /**
     * 감사 로그의 IP 는 플랫폼 콘솔 단일 출처인 이 인터셉터의 규칙을 재사용한다.
     * ★{@code ClientIpExtractor} 도 {@code SmsClientIpResolver} 도 쓰지 않는다(규칙 분기 금지).
     */
    private final PlatformOperatorGateInterceptor platformOperatorGateInterceptor;

    /** 현황 + 상태 + 임계값 1회 조회(화면 전체를 한 번에 채운다). */
    @GetMapping("/console")
    public ResponseEntity<?> getConsole() {

        SmsConsoleResponse response = platformSmsService.selectConsole();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 임계값 수정(즉시 반영 — 무캐시). */
    @PostMapping("/send-policy")
    public ResponseEntity<?> updateSendPolicy(
            @RequestBody SmsPolicyUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpServletRequest) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        platformSmsService.updatePolicy(
                SmsPolicyUpdateParam.from(request, tokenInfo.gv_userCd())
                , buildAuditContext(httpServletRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 킬스위치 수동 해제.
     * ★해제 경로는 여기 하나뿐이다. 자동 복구 경로를 추가하지 말 것(원인 미확인 재개 = 과금 재폭발).
     */
    @PostMapping("/kill-switch-release")
    public ResponseEntity<?> releaseKillSwitch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpServletRequest) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        platformSmsService.releaseKillSwitch(tokenInfo.gv_userCd(), buildAuditContext(httpServletRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 감사 컨텍스트(IP / User-Agent). 서비스가 HttpServletRequest 에 직접 의존하지 않게 한다. */
    private AuditContext buildAuditContext(HttpServletRequest request) {
        return new AuditContext(
                platformOperatorGateInterceptor.resolveClientIp(request)
                , request.getHeader("User-Agent"));
    }
}
