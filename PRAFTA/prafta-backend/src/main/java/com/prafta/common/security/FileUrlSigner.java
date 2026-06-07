package com.prafta.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 업로드 파일 서명 URL 발급/검증기.
 *
 * <p>플랫폼 업로드 파일 서빙 보안(서명 URL) 공통 인프라.
 * 페이로드 = 정규화 상대경로(relPath) + exp(epoch sec) + cmpnyCd 를 HMAC-SHA256 으로 서명한다.
 * 서명은 스코프 검증을 통과한 인증 API 응답 시점에만 발급되며, 서빙 필터({@code FileServingFilter})가
 * 동일 규칙으로 재계산해 검증한다.
 *
 * <p>설계:
 * <ul>
 *   <li>스코프 = 회사(cmpnyCd) 단위만 바인딩(siteCd 미바인딩).</li>
 *   <li>relPath 는 항상 {@code /uploads/{cmpnyCd}/...} 형태로 정규화(백슬래시/선두 슬래시/uploads 보정).</li>
 *   <li>cmpnyCd 는 relPath 의 첫 세그먼트와 동일(파일 소유 회사). 검증 시 path 세그먼트에서 추출한다.</li>
 *   <li>시크릿({@code file.sign.secret})이 비어 있으면(개발 환경) 서명 없이 절대 URL 만 발급한다
 *       — 필터 관대모드에서 미서명 URL 은 경고 로그 후 통과한다.</li>
 * </ul>
 *
 * <p>비밀(secret)·서명 원문은 절대 로그로 남기지 않는다.
 */
@Slf4j
@Component
public class FileUrlSigner {

    /** 서명 전용 시크릿(env FILE_SIGN_SECRET). jwt.secret 재사용 금지. 미설정 시 서명 비활성(관대모드 전제). */
    private final String secret;
    /** 서명 유효시간(초). 기본 1800. */
    private final long ttlSeconds;
    /** 절대 URL host(env FILE_PUBLIC_BASE_URL). 미설정 시 요청 헤더 폴백. */
    private final String publicBaseUrl;

    private static final String UPLOADS_PREFIX = "/uploads/";
    private static final String HMAC_ALG = "HmacSHA256";

    public FileUrlSigner(
            @Value("${file.sign.secret:}") String secret,
            @Value("${file.sign.ttl-seconds:1800}") long ttlSeconds,
            @Value("${file.public-base-url:}") String publicBaseUrl) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 1800;
        this.publicBaseUrl = publicBaseUrl;
        if (!StringUtils.hasText(secret)) {
            // 운영에서는 반드시 설정. 미설정 시 서명 미적용(관대모드 호환)임을 1회 경고.
            log.warn("[FileUrlSigner] file.sign.secret 미설정 — 서명 없이 절대 URL 만 발급합니다(관대모드 전제). 운영 배포 전 FILE_SIGN_SECRET 설정 필요.");
        }
    }

    /**
     * 서명 절대 URL 발급.
     *
     * @param relPath 파일 상대경로(예: {@code /uploads/001/20251120/00001/003/xxx.jpg} 또는 보정 대상)
     * @param cmpnyCd 파일 소유 회사코드(스코프 바인딩)
     * @return {@code {public-base}/uploads/...?exp=&sig=} 절대 URL. 시크릿 미설정 시 쿼리 없는 절대 URL.
     *         host 결정 불가 시 상대경로 폴백.
     */
    public String sign(String relPath, String cmpnyCd) {
        if (!StringUtils.hasText(relPath)) {
            return null;
        }
        String norm = normalize(relPath);
        String base = resolveBaseUrl();

        // 시크릿 미설정(개발) — 서명 없이 절대 URL 만.
        if (!StringUtils.hasText(secret)) {
            return base + norm;
        }

        long exp = System.currentTimeMillis() / 1000L + ttlSeconds;
        String sig = hmacBase64Url(payload(norm, exp, cmpnyCd));
        return base + norm + "?exp=" + exp + "&sig=" + sig;
    }

    /**
     * 서명 검증.
     *
     * @param relPath          서빙 요청 경로(정규화 전 가능). 내부에서 동일 규칙으로 정규화.
     * @param exp              만료 epoch(초)
     * @param sig              서명(Base64URL)
     * @param cmpnyCdFromPath  요청 경로에서 추출한 회사코드({@code /uploads/{cmpnyCd}/...})
     * @return exp 미만료 + sig 일치면 true
     */
    public boolean verify(String relPath, long exp, String sig, String cmpnyCdFromPath) {
        if (!StringUtils.hasText(relPath) || !StringUtils.hasText(sig) || exp <= 0) {
            return false;
        }
        if (exp < System.currentTimeMillis() / 1000L) {
            return false; // 만료
        }
        String norm = normalize(relPath);
        String expected = hmacBase64Url(payload(norm, exp, cmpnyCdFromPath));
        return constantTimeEquals(expected, sig);
    }

    /** 요청 경로 {@code /uploads/{cmpnyCd}/...} 의 첫 세그먼트(회사코드) 추출. 없으면 null. */
    public String extractCmpnyCd(String relPath) {
        if (!StringUtils.hasText(relPath)) {
            return null;
        }
        String norm = normalize(relPath);
        String rest = norm.substring(UPLOADS_PREFIX.length()); // {cmpnyCd}/...
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : (rest.isEmpty() ? null : rest);
    }

    // ----------------------------------------------------------------------

    /** HMAC 페이로드: relPath|exp|cmpnyCd. */
    private String payload(String normRelPath, long exp, String cmpnyCd) {
        return normRelPath + "|" + exp + "|" + (cmpnyCd == null ? "" : cmpnyCd);
    }

    /**
     * 상대경로 정규화: 백슬래시→슬래시, 선두 슬래시 보장, {@code /uploads/} 접두 보정.
     * (Windows 저장 시 FILE_PATH 에 백슬래시가 섞일 수 있어 URL 안전 형태로 통일한다.)
     */
    private String normalize(String relPath) {
        String p = relPath.replace('\\', '/').trim();
        // 중복 슬래시 축약
        p = p.replaceAll("/{2,}", "/");
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (!p.startsWith(UPLOADS_PREFIX) && !p.equals("/uploads")) {
            p = UPLOADS_PREFIX + (p.startsWith("/") ? p.substring(1) : p);
        }
        return p;
    }

    /** host 결정: 설정값 우선, 없으면 요청 X-Forwarded-* / Host 폴백, 그래도 없으면 빈 문자열(상대 URL). */
    private String resolveBaseUrl() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return stripTrailingSlash(publicBaseUrl);
        }
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String proto = firstNonBlank(req.getHeader("X-Forwarded-Proto"), req.getScheme());
                String host = firstNonBlank(req.getHeader("X-Forwarded-Host"), req.getHeader("Host"));
                if (StringUtils.hasText(host)) {
                    return proto + "://" + host;
                }
            }
        } catch (Exception e) {
            log.debug("[FileUrlSigner] 요청 컨텍스트 host 폴백 실패 — 상대 URL 로 발급");
        }
        return ""; // 상대 URL 폴백
    }

    private String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String hmacBase64Url(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            // 키/알고리즘 환경 문제 → 즉시 실패가 맞음(원문/시크릿은 로그 금지).
            throw new IllegalStateException("Failed to compute file URL signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
