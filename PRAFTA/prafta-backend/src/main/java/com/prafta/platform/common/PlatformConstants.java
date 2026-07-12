package com.prafta.platform.common;

/**
 * 플랫폼 운영자(개발/운영자) 영역 공통 상수.
 *
 * <p>플랫폼 운영자는 어느 고객 회사에도 속하지 않는 상위 등급으로, 예약 회사코드
 * {@code prafta_system_admin} 로 식별한다. 신규 고객사 프로비저닝·글로벌 약관 관리 등
 * {@code /platformApi/**} 엔드포인트는 이 회사코드 토큰 + IP 허용목록으로만 접근 가능하다.
 *
 * <p>주의: 이 코드값은 JWT/응답에 평문으로 실리므로 "비밀"이 아니다. 실제 보안 경계는
 * {@code PlatformOperatorGateInterceptor}(서버측 인가 + IP 허용목록)에 있다. 값은 가독성·
 * 테넌트 번호공간('001','002'…)과의 분리를 위한 것이다.
 */
public final class PlatformConstants {

    private PlatformConstants() {
    }

    /** 플랫폼 운영자 예약 회사코드(고객 테넌트와 분리). */
    public static final String PLATFORM_CMPNY_CD = "prafta_system_admin";
}
