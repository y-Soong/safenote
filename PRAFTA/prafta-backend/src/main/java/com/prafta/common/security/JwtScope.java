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

    /**
     * 공지 첨부 다운로드 전용 단기 토큰 (PRAFTA-047, 만료 5분).
     *
     * <p>/notice01/file-download-token 으로 발급되어 /notice01/file-download 만 통과한다.
     * claim: gv_cmpnyCd / gv_userCd / gv_scope / gv_noticeId / gv_fileMgmtCd.
     */
    public static final String NOTICE_FILE_DL = "NOTICE_FILE_DL";

    /**
     * 자료실(Archive) 첨부 다운로드 전용 단기 토큰 (PRAFTA-053, 만료 5분).
     *
     * <p>/notice02/file-download-token 으로 발급되어 /notice02/file-download 만 통과한다.
     * 공지(NOTICE_FILE_DL)와 분리하여 자료실 다운로드 흐름을 구조적으로 구분한다(타입 누수 방지).
     * claim: gv_cmpnyCd / gv_userCd / gv_scope / gv_noticeId / gv_fileMgmtCd.
     */
    public static final String ARCHIVE_FILE_DL = "ARCHIVE_FILE_DL";

    /**
     * 기본 근무타입 설정 게이트 전용 단기 토큰 (PRAFTA-COM-008-E-8).
     *
     * <p>로그인 직후 DEFAULT_SCH_CD 미설정 + 교대팀 비소속이면 정식 토큰 대신 발급되어
     * /login/set-default-sch 만 통과한다. 설정 저장 + 즉시 생성 후 정식 LoginResponse 로 교체한다.
     * PHONE_AUTH(인증대기) 통과 후에만 평가되는 후순위 게이트(D-E2). claim: gv_cmpnyCd / gv_userCd / gv_scope.
     */
    public static final String DEFAULT_SCH = "DEFAULT_SCH";

    private JwtScope() {
        // 상수 카탈로그 — 인스턴스화 방지
    }
}
