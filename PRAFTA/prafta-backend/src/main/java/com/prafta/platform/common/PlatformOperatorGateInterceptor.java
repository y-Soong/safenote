package com.prafta.platform.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 플랫폼 운영자 전용 게이트(/platformApi/**).
 *
 * <p>2중 방어:
 * <ol>
 *   <li><b>IP 허용목록</b>({@code prafta.platform.allowed-ips}, 콤마구분) — 설정 시 강제.
 *       미설정(빈 값)이면 통과(개발 편의). 운영에서는 반드시 설정.</li>
 *   <li><b>운영자 인가</b> — JWT 회사코드가 {@code prafta_system_admin} 이어야 함.</li>
 * </ol>
 *
 * <p>메뉴 숨김은 발견 방지(UX)일 뿐이며, 실제 차단은 본 게이트가 서버측에서 강제한다.
 * 신규 고객사 생성/글로벌 약관 관리 등 초고권한 엔드포인트가 대상이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformOperatorGateInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** 허용 IP 목록(콤마구분). 비어 있으면 IP 검사 생략(개발). 운영에서는 환경변수로 주입. */
    @Value("${prafta.platform.allowed-ips:}")
    private String allowedIpsRaw;

    /**
     * 신뢰 프록시 목록(콤마구분). RemoteAddr 가 이 목록에 있을 때만 X-Forwarded-For 선두를
     * 클라이언트 IP 로 채택한다. 기본(빈 값)에서는 XFF 를 신뢰하지 않는다(fail-safe — XFF 위조로
     * IP 게이트 우회·열람 로그 IP 위조 방지, 보안 리뷰 V-1).
     */
    @Value("${prafta.platform.trusted-proxies:}")
    private String trustedProxiesRaw;

    /** allowed-ips 미설정 기동 경고 1회(보안 리뷰 V-4 — fail-open 상태 가시화). */
    @PostConstruct
    void warnIfAllowedIpsEmpty() {
        if (!StringUtils.hasText(allowedIpsRaw)) {
            log.warn("[플랫폼 게이트] prafta.platform.allowed-ips 미설정 — IP 허용목록 계층 비활성(운영 배포 전 설정 권장)");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1) IP 허용목록 — 설정된 경우에만 강제(fail-closed).
        if (StringUtils.hasText(allowedIpsRaw)) {
            String clientIp = resolveClientIp(request);
            if (!allowedIps().contains(clientIp)) {
                log.warn("[플랫폼 게이트] 비허용 IP 차단 - ip={}, uri={}", clientIp, request.getRequestURI());
                throw new ApiException(PlatformErrorCode.PLATFORM_403_002);
            }
        }

        // 2) 운영자 인가 — 토큰 회사코드 = prafta_system_admin.
        String authorization = request.getHeader("Authorization");
        TokenInfo token = null;
        try {
            if (StringUtils.hasText(authorization)) {
                token = jwtUtil.getAllClaimsAsMap(authorization);
            }
        } catch (Exception e) {
            token = null; // 파싱 실패(만료/위조) → 비운영자로 간주하여 차단.
        }
        if (token == null || !PlatformConstants.PLATFORM_CMPNY_CD.equals(token.gv_cmpnyCd())) {
            log.warn("[플랫폼 게이트] 비운영자 접근 차단 - cmpnyCd={}, uri={}",
                    token == null ? "(none)" : token.gv_cmpnyCd(), request.getRequestURI());
            throw new ApiException(PlatformErrorCode.PLATFORM_403_001);
        }
        return true;
    }

    /** 콤마구분 허용목록 파싱(공백 제거). */
    private Set<String> allowedIps() {
        return parseCsv(allowedIpsRaw);
    }

    /** 콤마구분 목록 파싱 공통(공백 제거·빈 토큰 제외). */
    private static Set<String> parseCsv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 클라이언트 IP 해석(보안 리뷰 V-1 — XFF 신뢰 검증):
     * RemoteAddr 가 신뢰 프록시 목록({@code prafta.platform.trusted-proxies})에 있을 때만
     * X-Forwarded-For 선두를 채택하고, 그 외에는 RemoteAddr 를 그대로 사용한다.
     * 기본(목록 빈 값)에서는 XFF 를 신뢰하지 않는다(fail-safe).
     *
     * <p>위치정보 열람 로그(TB_LOCATION_ACCESS_LOG.CLIENT_IP)도 게이트와 동일 규칙으로 IP 를
     * 기록해야 하므로 본 빈의 public 메서드로 공개한다(단일 출처 — 규칙 분기 금지).
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!parseCsv(trustedProxiesRaw).contains(remoteAddr)) {
            return remoteAddr; // 신뢰 프록시 경유가 아니면 XFF 불신(위조 가능 헤더).
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
