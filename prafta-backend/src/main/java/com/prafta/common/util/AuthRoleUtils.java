package com.prafta.common.util;

/**
 * 권한/역할 판정을 모은 중앙 집중식 유틸리티.
 *
 * <p>AUTH_CD 값은 JWT 클레임 {@code gv_authCd}에서 가져온다. 본 클래스의
 * 판정 메서드는 서비스 전반에서 역할 검사(role gating)의 단일 진실 원천(SSOT)이다.
 * 신규 역할이 추가되거나 "master"가 다른 이름으로 변경될 경우, 이 클래스만 수정하면 된다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class AuthRoleUtils {

    /** 회사 마스터 계정. */
    public static final String AUTH_MASTER = "master";

    /** HR 담당자. */
    public static final String AUTH_HR_MANAGER = "hr";

    private AuthRoleUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * {@code authCd}가 어떤 형태든 관리 역할(회사 마스터, 사이트 매니저,
     * 노드 매니저)을 의미하면 true를 반환한다. 관리자용 endpoint
     * (예: 작업자 대신 OT 등록)에서 사용된다.
     */
    public static boolean isManager(String authCd) {
        if (authCd == null || authCd.isEmpty()) return false;
        return AUTH_MASTER.equals(authCd)
            || AUTH_HR_MANAGER.equals(authCd);
    }
}
