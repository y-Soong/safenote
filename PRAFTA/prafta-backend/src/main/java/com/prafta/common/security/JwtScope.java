package com.prafta.common.security;

/**
 * JWT 임시 scope 토큰의 scope 값 상수 카탈로그 (PRAFTA-037-F8).
 *
 * <p>{@link JwtUtil#generateScopeToken} 으로 발급된 임시 토큰은 {@code gv_scope} 클레임에
 * 본 상수 값 중 하나를 가진다. 검증 측은 본 상수와 동등 비교한다.
 *
 * <p>새 임시 흐름(비밀번호 재설정/이메일 인증/디바이스 등록 등) 추가 시 본 카탈로그에
 * 상수 1줄 추가 + 발급/검증 측 import 1줄.
 */
public final class JwtScope {

    /** 휴대폰 본인인증 전용 (PRAFTA-036) — /login/verify-phone-auth 만 통과. */
    public static final String PHONE_AUTH = "PHONE_AUTH";

    /**
     * 마이페이지 휴대폰 변경 인증 전용 (prafta-app-010, D4).
     *
     * <p>PHONE_AUTH 와 분리한 이유: PHONE_AUTH 는 로그인 토큰 발급 흐름에 묶여 있고,
     * 마이페이지 휴대폰 변경은 로그인 토큰을 발급하지 않는 단발성 검증 토큰이다.
     * 이 scope 토큰은 /appApi/mypage/profile(PUT) 저장 시 휴대폰 변경 검증에만 사용된다.
     */
    public static final String PHONE_CHANGE_AUTH = "PHONE_CHANGE_AUTH";

    private JwtScope() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
