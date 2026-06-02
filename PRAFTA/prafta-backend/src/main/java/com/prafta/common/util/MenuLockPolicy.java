package com.prafta.common.util;

import java.util.Locale;
import java.util.Set;

/**
 * 역할(AUTH_CD) 기반 화면권한 "잠금(해제 불가)" 규칙의 단일 진실 원천(SSOT).
 *
 * <p>특정 역할은 일부 대메뉴(MENU_M_ID) 하위 화면의 화면접근(USE_YN)과 모든
 * 버튼권한(BTN_*)을 항상 'Y' 로 열어 둔 채 고정해야 한다(해제 불가, PRAFTA-042 D4/D6).
 * 본 클래스는 그 역할→잠금 대메뉴 매핑을 상수로 정의한다. 규칙이 정적이므로
 * 별도 설정 테이블을 두지 않는다(D6).
 *
 * <p>잠금 판정은 BE 저장 경로({@code User02ServiceImpl.updateAuthMenuInfo})에서
 * 강제 보정(D5)에 사용되고, FE(User_02.vue)는 동일 규칙으로 체크박스 비활성(UX 보조)만 한다.
 *
 * <p>⚠️ {@code MENU_M_ID} 표기는 DB 실제 값과 100% 일치해야 한다(BE는 equals 비교).
 * 실DB 확인 표기: {@code attd, baim, chkLst, login, nearMiss, risk, tbm, user}.
 */
public final class MenuLockPolicy {

    /**
     * hr 잠금 대메뉴(MENU_M_ID): 근태관리/기초정보관리/사용자관리 (PRAFTA-042 D1).
     * <p>L-1 수정: 대소문자 무시 비교를 위해 상수를 소문자로 보관한다(DB 콜레이션이
     * 대소문자 무시이므로 표기 차이로 잠금이 누락되는 것을 방지).
     */
    private static final Set<String> HR_LOCKED_MENU_M_IDS = Set.of(
            "attd", "baim", "user");

    /**
     * safe 잠금 대메뉴(MENU_M_ID): 순회점검/위험성평가/TBM관리/기초정보관리/사용자관리/아차사고관리
     * (PRAFTA-042 D2 — 기존 + nearMiss 추가). 소문자 보관(L-1).
     */
    private static final Set<String> SAFE_LOCKED_MENU_M_IDS = Set.of(
            "chklst", "risk", "tbm", "baim", "user", "nearmiss");

    private MenuLockPolicy() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 해당 역할({@code authCd})에서 대메뉴({@code menuMId})가 잠금 대상인지 판정한다.
     *
     * <ul>
     *   <li>master: 전 대메뉴 잠금(항상 true).</li>
     *   <li>hr: {@link #HR_LOCKED_MENU_M_IDS} 포함 여부.</li>
     *   <li>safe: {@link #SAFE_LOCKED_MENU_M_IDS} 포함 여부.</li>
     *   <li>그 외 역할: 잠금 없음(false).</li>
     * </ul>
     */
    public static boolean isLockedMenu(String authCd, String menuMId) {
        if (authCd == null || authCd.isEmpty()) {
            return false;
        }
        if (AuthRoleUtils.AUTH_MASTER.equals(authCd)) {
            return true;
        }
        if (menuMId == null || menuMId.isEmpty()) {
            return false;
        }
        // L-1: DB 콜레이션이 대소문자 무시이므로 입력을 소문자로 정규화 후 비교한다.
        String normalizedMenuMId = menuMId.toLowerCase(Locale.ROOT);
        if (AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)) {
            return HR_LOCKED_MENU_M_IDS.contains(normalizedMenuMId);
        }
        if (AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd)) {
            return SAFE_LOCKED_MENU_M_IDS.contains(normalizedMenuMId);
        }
        return false;
    }
}
