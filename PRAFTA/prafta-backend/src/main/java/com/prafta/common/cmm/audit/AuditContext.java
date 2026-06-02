package com.prafta.common.cmm.audit;

/**
 * 감사 적재 시 Controller 가 추출해 Service 에 전달하는 요청 컨텍스트 (PRAFTA-037-F5).
 *
 * <p>Service 계층이 {@code HttpServletRequest} 에 직접 의존하지 않도록 Controller 에서
 * IP / User-Agent 만 뽑아 본 레코드로 묶어 전달한다.
 *
 * <p>- IP 추출 우선순위: {@code X-Forwarded-For} 첫 IP → {@code getRemoteAddr()} (실패 시 null).
 * <p>- User-Agent: 헤더 값 그대로 (≤500자 절단은 호출 측 책임).
 */
public record AuditContext(
        String ipAddress
        , String userAgent
) {
}
