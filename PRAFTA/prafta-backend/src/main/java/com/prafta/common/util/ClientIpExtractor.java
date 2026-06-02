package com.prafta.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트 IP 추출 유틸 (PRAFTA-037-F5).
 *
 * <p>{@code X-Forwarded-For} 헤더가 있으면 첫 IP(콤마 분리)를 우선 반환 — 프록시/로드밸런서 환경 대응.
 * 없으면 {@link HttpServletRequest#getRemoteAddr()}. 둘 다 비어 있으면 {@code null}.
 *
 * <p>IPv6 지원 (`tb_audit_log.IP_ADDRESS varchar(45)`).
 */
public final class ClientIpExtractor {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpExtractor() {
        // 유틸 — 인스턴스화 방지
    }

    /**
     * 요청에서 클라이언트 IP 를 추출한다. 추출 실패 시 {@code null}.
     */
    public static String extract(HttpServletRequest request) {
        if (request == null) return null;

        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            // 콤마 구분된 다중 IP 중 첫 IP 가 원본 클라이언트 IP.
            int commaIdx = xff.indexOf(',');
            String first = (commaIdx > 0) ? xff.substring(0, commaIdx) : xff;
            String trimmed = first.trim();
            if (!trimmed.isEmpty()) return trimmed;
        }

        String remote = request.getRemoteAddr();
        if (remote != null && !remote.isBlank()) {
            return remote;
        }

        return null;
    }
}
