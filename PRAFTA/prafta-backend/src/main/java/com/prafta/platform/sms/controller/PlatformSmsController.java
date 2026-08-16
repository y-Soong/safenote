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
import com.prafta.platform.sms.application.param.SmsHistoryListParam;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.dto.request.SmsHistoryListRequest;
import com.prafta.platform.sms.dto.request.SmsPolicyUpdateRequest;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;
import com.prafta.platform.sms.dto.response.SmsHistoryListResponse;
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

    /**
     * 발송 이력 목록 조회(기간 필터 + 서버 페이징).
     *
     * <p>최종 경로: {@code POST /prafta/platformApi/sms/send-histories/search}
     *
     * <p>★<b>조회인데 POST 인 이유</b> — 검색 조건에 <b>휴대폰 평문</b>이 들어간다. GET 쿼리스트링으로
     * 받으면 서버 코드가 로그를 남기지 않아도 <b>nginx/ALB access log · CloudFront 로그 · 브라우저
     * 히스토리·리퍼러</b>에 평문 휴대폰이 복제된다. 이들은 애플리케이션 로그와 보존주기·접근통제가
     * 달라 사후 회수가 어렵다(정책 §11.1 최소 수집 / 평문 PII 로깅 금지).
     * 바디로 받으면 그 경로가 전부 사라진다. <b>휴대폰 조건을 쿼리스트링으로 되돌리지 말 것.</b>
     *
     * <p>★★<b>인가는 {@code PlatformOperatorGateInterceptor}(경로 기반)가 강제한다 — 어노테이션이 없다.</b>
     * 이 클래스가 {@code com.prafta.platform.*} 아래에 있어 {@code /prafta/platformApi/**} 로 매핑되기 때문에
     * 게이트가 자동 적용된다({@code gv_cmpnyCd != prafta_system_admin} 이면 {@code PLATFORM_403_001}).
     * <b>같은 서비스/매퍼를 {@code web}/{@code app} 컨트롤러로 옮기면 게이트가 사라진다</b> —
     * {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 가 없어 회사 경계 없이 전 고객사 휴대폰이 샌다.
     *
     * <p>★<b>감사 대상이 아니므로 {@code HttpServletRequest} 를 받지 않는다</b>(마스킹 목록 조회 —
     * 공통 정책서 §11.3 대상 목록 어디에도 해당하지 않는다. User_09 처리 이력 조회 선례와 동일).
     * {@code buildAuditContext} 를 호출하지 않는다.
     *
     * <p>★응답에는 인증번호({@code AUTH_CD})·{@code MBL_NO_HMAC}·{@code SEND_IP_HASH} 가 없다.
     * 휴대폰은 서버에서 복호 후 마스킹된 문자열만 내려간다.
     */
    @PostMapping("/send-histories/search")
    public ResponseEntity<?> getSendHistories(
            @RequestBody SmsHistoryListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        SmsHistoryListResponse response =
                platformSmsService.selectSendHistory(SmsHistoryListParam.from(request, tokenInfo));

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
