package com.prafta.common.cmm.sms.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.prafta.common.security.crypto.HmacSigner;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMS 발송 상한의 <b>IP 축 전용</b> 클라이언트 IP 해석기(SMS2-B2).
 *
 * <p>★기존 두 구현을 재사용하지 않고 별도로 만든 이유
 * <ul>
 *   <li>{@code common.util.ClientIpExtractor.extract()} — X-Forwarded-For 의 <b>첫 IP 를 무검증 채택</b>한다.
 *       헤더 한 줄로 위조 가능해 상한 우회에 그대로 쓸 수 없다.
 *       ★감사로그·로그인·근태가 이미 쓰고 있어 파급이 크므로 <b>수정하지 않는다</b>(별건 백로그).</li>
 *   <li>{@code PlatformOperatorGateInterceptor.resolveClientIp()} — 신뢰 프록시 검증 계층은 옳지만
 *       그 뒤에 XFF <b>선두</b>를 채택한다. CloudFront 토폴로지에서는 선두가 클라이언트 위조분이라
 *       여전히 위조 가능하다. ★플랫폼 콘솔 IP 기록의 단일 출처라 <b>수정하지 않는다</b>(별건 백로그).</li>
 * </ul>
 * 본 클래스는 위 둘과 완전히 독립이며, 오른쪽(=우리 인프라 쪽)에서 세는 방식을 쓴다.
 *
 * <p><b>실제 토폴로지</b>
 * <pre>
 *   클라이언트 → CloudFront(서울 엣지) → origin.prafta.com = EC2 nginx :443 → Spring Boot :8080
 * </pre>
 * CloudFront 는 뷰어 XFF <b>뒤에</b> 뷰어 IP 를 덧붙이고, nginx 가 {@code proxy_add_x_forwarded_for} 면
 * 엣지 IP 가 한 번 더 붙는다. 따라서 예상 형태는
 * {@code [클라 위조분 …] , [진짜 클라 IP] , [CloudFront 엣지 IP]} 이며 진짜 클라 IP 는 오른쪽에서 2번째다.
 *
 * <p>★★<b>fail-open 원칙(절대 규칙)</b><br>
 * IP 를 신뢰 수준으로 확정하지 못하면 <b>IP 축 제한을 건너뛴다</b>(null 반환).
 * fail-closed 로 하면 전 사용자가 동일 IP(=nginx)로 보이는 상황에서 서비스가 전면 마비된다.
 * 번호 축·사용자 축·전역 축이 주 방어이며 IP 축은 보조다.
 *
 * <p>★<b>1단계는 관측 전용</b>이다. 본 클래스가 해시를 계산해 적재하고 XFF 구조를 진단 로그로 남기지만,
 * 실제 차단 여부는 정책행의 {@code IP_AXIS_ENABLED_YN}(기본 'N')이 결정한다
 * ({@link SmsSendPolicy#isIpAxisEnforced()}). nginx 설정은 레포 밖(EC2 로컬)이라
 * XFF 가 누적인지 덮어쓰기인지 정적으로 확인할 수 없기 때문이다.
 *
 * <p>★IP 평문은 DB·로그 어디에도 남기지 않는다(공통 정책서 §11.1 최소 수집).
 *    저장·비교는 HMAC 해시로만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsClientIpResolver {

    /**
     * HMAC 도메인 구분자.
     * ★필수 — 휴대폰 HMAC 과 같은 pepper 키를 쓰므로, 도메인을 분리하지 않으면
     *   "같은 문자열이면 같은 해시" 라는 성질 때문에 교차 사전공격 표면이 생긴다.
     */
    private static final String IP_HASH_DOMAIN = "sms-ip:";

    private final HmacSigner hmacSigner;

    /**
     * Spring 에 직접 연결하는 신뢰 프록시(=nginx) 주소 목록(콤마구분).
     * ★비어 있으면 XFF 를 신뢰하지 않는다(= IP 축 전면 스킵). 기본 빈값이라 로컬·개발은 무회귀다.
     */
    @Value("${prafta.sms.ip.trusted-proxies:}")
    private String trustedProxiesRaw;

    /**
     * XFF 우측에서 건너뛸 자사 인프라 홉 수. CloudFront 엣지 1홉 → 1.
     * ★운영 진단 로그로 실제 토큰 구성을 확인한 뒤 확정할 것.
     */
    @Value("${prafta.sms.ip.trusted-hops:1}")
    private int trustedHops;

    /** 진단 로그 출력 여부(1단계 관측용). 홉 수 확정 후 false 로 내린다. */
    @Value("${prafta.sms.ip.diagnostic:true}")
    private boolean diagnosticEnabled;

    /**
     * 요청에서 신뢰 가능한 클라이언트 IP 를 뽑아 HMAC 해시를 반환한다.
     *
     * @return IP 해시(Base64Url 43자). <b>확정하지 못하면 null</b>(= IP 축 스킵)
     */
    public String resolveIpHash(HttpServletRequest request) {
        if (request == null) {
            return skip("NO_REQUEST");
        }

        String remoteAddr = request.getRemoteAddr();

        // ★★[3차 / sec N-6] getHeader() 가 아니라 getHeaders() 로 <b>전부</b> 읽는다.
        //   getHeader() 는 동명 헤더가 여러 개일 때 <b>첫 번째만</b> 반환한다.
        //   프록시가 XFF 를 병합하지 않고 별도 헤더 줄로 덧붙이는 구성이면, 클라이언트가 보낸
        //   위조 XFF 가 첫 줄이 되어 <b>통째로 채택</b>된다 — 우측 인덱싱 방어가 무의미해진다.
        //   구성이 정적으로 확인 불가하므로, 2개 이상이면 신뢰하지 않고 축을 건너뛴다(fail-open).
        List<String> xffHeaders = collectHeaders(request, "X-Forwarded-For");
        String xff = xffHeaders.size() == 1 ? xffHeaders.get(0) : null;

        logDiagnostic(remoteAddr, xff, xffHeaders.size());

        // 1) 신뢰 프록시 경유가 아니면 XFF 를 믿지 않는다.
        //    ★remoteAddr 자체를 클라이언트로 채택하지도 않는다 — 운영에서는 그 값이 nginx 주소라
        //      전 사용자가 하나의 버킷으로 묶여 오차단이 난다.
        if (!StringUtils.hasText(trustedProxiesRaw)) {
            return skip("NO_TRUSTED_PROXY_CONFIG");
        }
        if (!parseCsv(trustedProxiesRaw).contains(remoteAddr)) {
            return skip("UNTRUSTED_REMOTE");
        }

        // 2) XFF 가 여러 줄이면(N-6) 어느 줄이 프록시가 붙인 것인지 확정할 수 없다.
        if (xffHeaders.size() > 1) {
            return skip("MULTIPLE_XFF");
        }

        // 3) XFF 부재면 확정 불가.
        if (!StringUtils.hasText(xff)) {
            return skip("NO_XFF");
        }

        // 4) ★오른쪽에서 센다. 왼쪽(선두)은 클라이언트가 임의로 채워 넣을 수 있는 구간이다.
        String[] tokens = xff.split(",");
        int idx = tokens.length - 1 - trustedHops;
        if (idx < 0) {
            return skip("HOP_OVERFLOW");
        }

        String candidate = normalizeIpToken(tokens[idx]);
        if (candidate == null) {
            return skip("INVALID_IP");
        }

        // 5) 사설/루프백/링크로컬이 채택 위치에 오면 = 우리 인프라 주소를 클라이언트로 오인한 상태다.
        //    홉 수가 틀렸다는 신호이므로 신뢰하지 않는다.
        if (isPrivateOrLocal(candidate)) {
            return skip("PRIVATE_IP");
        }

        return hmacSigner.hmacSha256Base64Url(IP_HASH_DOMAIN + candidate);
    }

    /**
     * IP 축 스킵. ★사유는 열거형 문자열만 남긴다 — IP 값은 절대 로그에 넣지 않는다.
     *
     * <p>★[3차 / sec N-11 · qa Q-6] {@code warn} → {@code debug} 로 내렸다.
     *    {@code trusted-proxies} 미설정이 전 환경의 기본값이라 <b>모든 발송 요청이 100% WARN</b> 을 찍었고,
     *    그 소음이 진짜 경고를 가렸다(운영은 TRACE + p6spy 상태라 부담이 겹친다).
     *    "IP 축이 꺼져 있다" 는 사실은 상태이지 사건이 아니므로 요청마다 알릴 필요가 없다 —
     *    대신 기동 시 1회만 WARN 으로 알린다({@link #warnIpAxisConfigOnStartup()}).
     */
    private String skip(String reason) {
        log.debug("[SMS상한:IP축] 클라이언트 IP 미확정 - IP 축 스킵, 사유={}", reason);
        return null;
    }

    /**
     * [3차 / sec N-11] 기동 시 1회 IP 축 설정 상태 경고.
     *
     * <p>미설정이면 {@code SEND_IP_HASH} 가 항상 NULL 이라 <b>관측 데이터가 하나도 쌓이지 않는다</b>.
     * 2단계(IP 축 차단 전환)에 필요한 홉 수를 영영 확정할 수 없으므로, 이 상태를 기동 로그에 남긴다.
     */
    @PostConstruct
    void warnIpAxisConfigOnStartup() {
        if (!StringUtils.hasText(trustedProxiesRaw)) {
            log.warn("[SMS상한:IP축] prafta.sms.ip.trusted-proxies 미설정 - IP 축 전면 스킵(SEND_IP_HASH 항상 NULL)."
                    + " 로컬/개발은 정상이나, 운영에서 2단계(IP 축 차단)로 가려면 nginx 주소를"
                    + " SMS_IP_TRUSTED_PROXIES 로 주입해야 한다. 요청 단위 로그는 debug 로 내려 두었다");
            return;
        }
        log.info("[SMS상한:IP축] 신뢰 프록시 {}건 설정됨, trustedHops={}, 진단로그={}",
                parseCsv(trustedProxiesRaw).size(), trustedHops, diagnosticEnabled);
    }

    /**
     * 운영 XFF 구조 계측용 진단 로그(SMS2-B2 1단계).
     *
     * <p>★평문 IP 를 절대 출력하지 않는다. 토큰 개수와 각 토큰의 <b>분류</b>(PRIVATE/PUBLIC/INVALID)만 남긴다.
     *    이 두 정보만으로 홉 수를 확정할 수 있다.
     * <p>판독법
     * <ul>
     *   <li>{@code xffTokenCount=2, kinds=[PUBLIC, PUBLIC]} → 정상. 오른쪽 1개가 CloudFront 엣지 → hops=1.</li>
     *   <li>{@code xffTokenCount=1, kinds=[PUBLIC]} → ★nginx 가 XFF 를 덮어쓰고 있다(엣지 IP 만 남음).
     *       이 경우 <b>IP 축은 사용 불가</b>다. nginx 를 {@code proxy_add_x_forwarded_for} 로 바꾸는 것은
     *       인프라 별건이며, 그 전까지 {@code IP_AXIS_ENABLED_YN} 을 'N' 으로 유지한다.</li>
     *   <li>{@code remoteAddrPrivate=false} → nginx 를 거치지 않고 직접 들어온 요청이다(설정 점검 필요).</li>
     *   <li>{@code xffHeaderCount>1} → ★프록시가 XFF 를 병합하지 않는다. 이 상태로는 IP 축 사용 불가(sec N-6).</li>
     * </ul>
     *
     * <p>★[3차 / sec N-11] {@code info} → {@code debug}. 계측이 필요할 때만
     *    {@code logging.level.com.prafta.common.cmm.sms.policy=DEBUG} 로 켠다
     *    (프로퍼티 {@code prafta.sms.ip.diagnostic} 는 그대로 상위 게이트로 남는다).
     */
    private void logDiagnostic(String remoteAddr, String xff, int xffHeaderCount) {
        if (!diagnosticEnabled) {
            return;
        }
        String[] tokens = StringUtils.hasText(xff) ? xff.split(",") : new String[0];
        StringBuilder kinds = new StringBuilder("[");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                kinds.append(", ");
            }
            String ip = normalizeIpToken(tokens[i]);
            kinds.append(ip == null ? "INVALID" : (isPrivateOrLocal(ip) ? "PRIVATE" : "PUBLIC"));
        }
        kinds.append("]");

        String normalizedRemote = normalizeIpToken(remoteAddr);
        log.debug("[SMS상한:IP축진단] remoteAddrPrivate={}, xffHeaderCount={}, xffTokenCount={},"
                + " tokenKinds={}, trustedHops={}",
            normalizedRemote != null && isPrivateOrLocal(normalizedRemote),
            xffHeaderCount, tokens.length, kinds, trustedHops);
    }

    /** 동명 헤더 전부를 순서대로 수집한다(sec N-6 — {@code getHeader()} 는 첫 줄만 반환한다). */
    private static List<String> collectHeaders(HttpServletRequest request, String name) {
        List<String> values = new ArrayList<>();
        Enumeration<String> e = request.getHeaders(name);
        if (e == null) {
            return values;
        }
        while (e.hasMoreElements()) {
            String v = e.nextElement();
            if (StringUtils.hasText(v)) {
                values.add(v);
            }
        }
        return values;
    }

    /**
     * XFF 토큰 정규화. 공백 제거 + IPv6 대괄호/포트 부착 형태를 벗겨낸다.
     * 유효한 IP 리터럴이 아니면 null.
     */
    private String normalizeIpToken(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }

        // "[::1]:8080" / "[::1]" 형태
        if (v.startsWith("[")) {
            int close = v.indexOf(']');
            if (close < 0) {
                return null;
            }
            v = v.substring(1, close);
        } else if (v.indexOf(':') == v.lastIndexOf(':') && v.indexOf(':') > 0) {
            // "1.2.3.4:5678" 형태(콜론이 1개 = IPv4+포트). IPv6 는 콜론이 2개 이상이라 여기 안 걸린다.
            v = v.substring(0, v.indexOf(':'));
        }

        if (v.isEmpty()) {
            return null;
        }
        return isIpLiteral(v) ? v : null;
    }

    /** IPv4 점표기 또는 IPv6(콜론 포함) 리터럴인지. ★DNS 조회를 하지 않는다(외부 입력으로 조회 유발 금지). */
    private boolean isIpLiteral(String v) {
        if (v.indexOf(':') >= 0) {
            // IPv6: 16진수·콜론만 허용(축약형 :: 포함).
            return v.chars().allMatch(c -> Character.digit(c, 16) >= 0 || c == ':');
        }
        String[] parts = v.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3 || !p.chars().allMatch(Character::isDigit)) {
                return false;
            }
            if (Integer.parseInt(p) > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * 사설/루프백/링크로컬/예약 대역 판정.
     *
     * <p>IPv4: {@code 0/8}, {@code 10/8}, {@code 100.64/10}(CGNAT), {@code 127/8},
     * {@code 169.254/16}, {@code 172.16/12}, {@code 192.0.2/24}(TEST-NET-1), {@code 192.168/16}<br>
     * IPv6: {@code ::1}, {@code ::ffff:127.*}, {@code fc00::/7}, {@code fe80::/10}
     *
     * <p>★[3차 / sec N-12] {@code 100.64.0.0/10}(CGNAT)·{@code 0.0.0.0/8}·{@code 192.0.2.0/24} 를 추가했다.
     *    CGNAT 는 통신사 NAT 대역이라 채택 위치에 오면 홉 수가 틀렸다는 신호이고,
     *    나머지 둘은 정상 클라이언트 주소가 될 수 없는 예약 대역이다.
     *    이 대역들이 채택되면 서로 다른 사용자가 한 버킷으로 묶여 오차단이 난다.
     */
    private boolean isPrivateOrLocal(String ip) {
        if (ip.indexOf(':') >= 0) {
            String v6 = ip.toLowerCase();
            if ("::1".equals(v6) || v6.startsWith("::ffff:127.")) {
                return true;
            }
            // Unique Local Address fc00::/7 (fc.. / fd..) 및 링크로컬 fe80::/10
            return v6.startsWith("fc") || v6.startsWith("fd") || v6.startsWith("fe8")
                || v6.startsWith("fe9") || v6.startsWith("fea") || v6.startsWith("feb");
        }

        String[] parts = ip.split("\\.");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[1]);
        int c = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;
        // 0.0.0.0/8(현재 네트워크 - 유효한 출발지가 될 수 없다) / 10.0.0.0/8 / 127.0.0.0/8
        if (a == 0 || a == 10 || a == 127) {
            return true;
        }
        // 100.64.0.0/10 — CGNAT(통신사 대규모 NAT). 채택 위치에 오면 홉 수 오설정 신호다.
        if (a == 100 && b >= 64 && b <= 127) {
            return true;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return true;
        }
        // 192.0.2.0/24 — TEST-NET-1(문서화 예약). 192.168/16 보다 먼저 검사할 필요는 없으나 함께 둔다.
        if (a == 192 && b == 0 && c == 2) {
            return true;
        }
        if (a == 192 && b == 168) {
            return true;
        }
        return a == 169 && b == 254;
    }

    /** 콤마구분 목록 파싱(공백 제거·빈 토큰 제외). */
    private static Set<String> parseCsv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
