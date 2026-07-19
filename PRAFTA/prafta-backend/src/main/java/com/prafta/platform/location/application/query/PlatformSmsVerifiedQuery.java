package com.prafta.platform.location.application.query;

/**
 * 플랫폼 위치열람 SMS 인증 상태(10분 유효) 판정 쿼리 파라미터.
 *
 * <p>운영자 본인 HMAC 은 TB_USER 조인으로 서버가 해석한다(클라 입력 없음).
 */
public record PlatformSmsVerifiedQuery(
    String cmpnyCd
    , String userCd
) {
}
