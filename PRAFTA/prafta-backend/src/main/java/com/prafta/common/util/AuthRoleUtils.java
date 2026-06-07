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

    /** 안전관리자. */
    public static final String AUTH_SAFETY_MANAGER = "safe";

    /** 시스템(전역 운영) 계정. */
    public static final String AUTH_SYSTEM = "system";

    /** 권한 미부여(접근 차단) 계정. */
    public static final String AUTH_NONE = "999999";

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

    /**
     * 회사 공통(전사) 리소스를 등록/수정할 수 있는 역할인지 판정한다.
     * (master 또는 safe). TBM 콘텐츠 라이브러리(prafta-033-A)에서 회사공통
     * 콘텐츠(SITE_CD IS NULL) 저장 권한 게이트로 사용된다.
     */
    public static boolean canManageCommon(String authCd) {
        if (authCd == null || authCd.isEmpty()) return false;
        return AUTH_MASTER.equals(authCd)
            || AUTH_SAFETY_MANAGER.equals(authCd);
    }

    /**
     * 회사 전체 스코프(공통 + 모든 사업장)를 조회/관리할 수 있는 역할인지 판정한다.
     * (master 또는 safe). 사업장 관리자/일반 사용자는 자기 사업장 + 회사공통만.
     */
    public static boolean isCompanyWide(String authCd) {
        return canManageCommon(authCd);
    }

    /**
     * 접근 차단 권한(999999)인지 판정한다. true면 해당 화면/API 진입을 거부한다.
     */
    public static boolean isAccessDenied(String authCd) {
        return AUTH_NONE.equals(authCd);
    }

    /**
     * 모든 노드(부서)를 전사 범위로 관리할 수 있는 역할인지 판정한다.
     * (master / hr / safe). PRAFTA-042-6: 노드 관리 스코프 판정에 safe 를 포함시키기 위해 신설.
     *
     * <p>⚠️ {@link #isManager(String)}(master/hr)와 의미가 다르다 — isManager 는 OT 대리등록 등
     * 다른 관리 동작에 쓰이므로 변경하지 않는다. 본 헬퍼는 노드 스코프 전사 통과 판정 전용이다.
     */
    public static boolean canManageAllNodes(String authCd) {
        if (authCd == null || authCd.isEmpty()) return false;
        return AUTH_MASTER.equals(authCd)
            || AUTH_HR_MANAGER.equals(authCd)
            || AUTH_SAFETY_MANAGER.equals(authCd);
    }

    /**
     * 시스템 운영 권한(master / system)인지 판정한다. 삭제 등 파괴적(destructive) 동작의
     * 서버 게이트에 사용한다.
     *
     * <p>출처: prafta-048 권한 시드(tb_syst_auth_menu, MENU_D_ID='Acct_01')의 BTN_DELT='Y'
     * 행은 master/system 두 역할뿐이다. MenuLockPolicy 상 master 는 모든 BTN_* 가 강제 'Y' 로
     * 보정되므로 정합을 유지한다.
     */
    public static boolean isSystemOperator(String authCd) {
        if (authCd == null || authCd.isEmpty()) return false;
        return AUTH_MASTER.equals(authCd)
            || AUTH_SYSTEM.equals(authCd);
    }

    /**
     * 안전관리 도메인의 등록/저장(BTN_NEW / BTN_SAVE) 권한 역할인지 판정한다.
     * (master / hr / safe / system).
     *
     * <p>출처: prafta-048 권한 시드(tb_syst_auth_menu, MENU_D_ID='Acct_01')의 BTN_NEW='Y'
     * 및 BTN_SAVE='Y' 행은 master/hr/safe/system 네 역할이다. 사원권한(00001 등)·미부여(99999)는
     * BTN_NEW/SAVE='N' 이므로 제외된다. master 는 MenuLockPolicy 강제 'Y' 보정 대상이라 정합.
     */
    public static boolean canWriteSafetyContent(String authCd) {
        if (authCd == null || authCd.isEmpty()) return false;
        return AUTH_MASTER.equals(authCd)
            || AUTH_HR_MANAGER.equals(authCd)
            || AUTH_SAFETY_MANAGER.equals(authCd)
            || AUTH_SYSTEM.equals(authCd);
    }
}
