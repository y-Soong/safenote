package com.prafta.common.util;

import java.util.Locale;
import java.util.Set;

/**
 * 역할(AUTH_CD) 기반 화면권한 "잠금(해제 불가)" 규칙의 단일 진실 원천(SSOT).
 *
 * <p>특정 역할은 일부 화면의 화면접근(USE_YN)과 모든 버튼권한(BTN_*)을 항상 'Y' 로
 * 열어 둔 채 고정해야 한다(해제 불가, PRAFTA-042 D4/D6).
 *
 * <h3>LNB 재편(lnb-restructure)에 따른 잠금 기준 전환 — MENU_D_ID 기준</h3>
 * <p>기존에는 잠금을 대메뉴(MENU_M_ID = attd/baim/user/chkLst/risk/tbm/nearMiss) 단위로
 * 판정했다. 그러나 LNB 재편으로 한 대분류 탭(attdHr/safety/system/dailyAcct)에 여러 도메인이
 * 섞이게 되어 MENU_M_ID 단위 잠금은 "어떤 화면이 잠겼는지"의 원래 의도를 더 이상 정확히
 * 표현하지 못한다(예: system 탭에 user + baim + notice 가 공존 → MENU_M='system' 잠금은
 * 의도 초과/누락 양쪽 위험).
 *
 * <p>따라서 잠금 기준을 <b>MENU_D_ID(화면 식별자) 단위</b>로 전환한다. MENU_D_ID 는 LNB 재편
 * 후에도 불변(라우트·권한 보존)이므로, 구 잠금 의도를 1:1 보존할 수 있다.
 * 잠금 대상 MENU_D_ID 집합은 구 MENU_M_ID 잠금셋(HR={attd,baim,user},
 * SAFE={chkLst,risk,tbm,baim,user,nearMiss})에 소속되던 화면들을 그대로 옮긴 것이다.
 *
 * <p>잠금 판정은 BE 저장 경로({@code User02ServiceImpl.updateAuthMenuInfo})에서 강제 보정(D5)에
 * 사용되고, FE(User_02.vue)는 동일 규칙으로 체크박스 비활성(UX 보조)만 한다.
 *
 * <p>⚠️ {@code MENU_D_ID} 표기는 DB 실제 값과 100% 일치해야 한다(BE는 equals 비교).
 * DB 콜레이션이 대소문자 무시(utf8mb4_unicode_ci)이므로 비교 전 소문자 정규화한다.
 */
public final class MenuLockPolicy {

    // ------------------------------------------------------------------------
    // [MENU_D_ID 기준 잠금셋] — 소문자 보관(L-1: 콜레이션 대소문자 무시 정합).
    // ------------------------------------------------------------------------

    /**
     * hr 잠금 화면(MENU_D_ID): 구 MENU_M_ID 잠금셋 {attd, baim, user} 소속 화면 1:1 이전.
     * <ul>
     *   <li>attd : Attd_01,02,03,05,06,07,08,09,10,11,12,13,14, LeavePromotion_01</li>
     *   <li>baim : Baim_01,02,03,05,06,07</li>
     *   <li>user : User_01,02,03,04,05</li>
     * </ul>
     */
    private static final Set<String> HR_LOCKED_MENU_D_IDS = Set.of(
            // attd
            "attd_01", "attd_02", "attd_03", "attd_05", "attd_06", "attd_07",
            "attd_08", "attd_09", "attd_10", "attd_11", "attd_12", "attd_13", "attd_14",
            "leavepromotion_01",
            // baim
            "baim_01", "baim_02", "baim_03", "baim_05", "baim_06", "baim_07",
            // user
            "user_01", "user_02", "user_03", "user_04", "user_05");

    /**
     * safe 잠금 화면(MENU_D_ID): 구 MENU_M_ID 잠금셋 {chkLst, risk, tbm, baim, user, nearMiss}
     * 소속 화면 1:1 이전.
     * <ul>
     *   <li>chkLst   : ChkLst_01,02,03,04</li>
     *   <li>risk     : Risk_01,02,03</li>
     *   <li>tbm      : Tbm_01,02,03,04</li>
     *   <li>baim     : Baim_01,02,03,05,06,07</li>
     *   <li>user     : User_01,02,03,04,05</li>
     *   <li>nearMiss : NearMiss_01</li>
     * </ul>
     */
    private static final Set<String> SAFE_LOCKED_MENU_D_IDS = Set.of(
            // chkLst
            "chklst_01", "chklst_02", "chklst_03", "chklst_04",
            // risk
            "risk_01", "risk_02", "risk_03",
            // tbm
            "tbm_01", "tbm_02", "tbm_03", "tbm_04",
            // baim
            "baim_01", "baim_02", "baim_03", "baim_05", "baim_06", "baim_07",
            // user
            "user_01", "user_02", "user_03", "user_04", "user_05",
            // nearMiss
            "nearmiss_01");

    // ------------------------------------------------------------------------
    // [구 MENU_M_ID 기준 잠금셋] — @Deprecated 하위 호환 보존.
    // ------------------------------------------------------------------------

    /** @deprecated MENU_D_ID 기준({@link #HR_LOCKED_MENU_D_IDS})으로 전환. 하위 호환 보존. */
    @Deprecated
    private static final Set<String> HR_LOCKED_MENU_M_IDS = Set.of(
            "attd", "baim", "user");

    /** @deprecated MENU_D_ID 기준({@link #SAFE_LOCKED_MENU_D_IDS})으로 전환. 하위 호환 보존. */
    @Deprecated
    private static final Set<String> SAFE_LOCKED_MENU_M_IDS = Set.of(
            "chklst", "risk", "tbm", "baim", "user", "nearmiss");

    private MenuLockPolicy() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 해당 역할({@code authCd})에서 화면({@code menuDId})이 잠금 대상인지 판정한다(MENU_D_ID 기준).
     *
     * <ul>
     *   <li>master: 전 화면 잠금(항상 true).</li>
     *   <li>hr: {@link #HR_LOCKED_MENU_D_IDS} 포함 여부.</li>
     *   <li>safe: {@link #SAFE_LOCKED_MENU_D_IDS} 포함 여부.</li>
     *   <li>그 외 역할: 잠금 없음(false).</li>
     * </ul>
     */
    public static boolean isLockedMenuByMenuDId(String authCd, String menuDId) {
        if (authCd == null || authCd.isEmpty()) {
            return false;
        }
        if (AuthRoleUtils.AUTH_MASTER.equals(authCd)) {
            return true;
        }
        if (menuDId == null || menuDId.isEmpty()) {
            return false;
        }
        // DB 콜레이션이 대소문자 무시이므로 입력을 소문자로 정규화 후 비교한다.
        String normalized = menuDId.toLowerCase(Locale.ROOT);
        if (AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)) {
            return HR_LOCKED_MENU_D_IDS.contains(normalized);
        }
        if (AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd)) {
            return SAFE_LOCKED_MENU_D_IDS.contains(normalized);
        }
        return false;
    }

    /**
     * 해당 역할({@code authCd})에서 대메뉴({@code menuMId})가 잠금 대상인지 판정한다.
     *
     * @deprecated LNB 재편 이후 한 대분류 탭에 여러 도메인이 섞여 MENU_M_ID 단위 잠금은
     *             원래 의도를 정확히 표현하지 못한다. 신규 코드는
     *             {@link #isLockedMenuByMenuDId(String, String)} 를 사용할 것.
     *             master 전체잠금 판정(menuMId=null) 용도로만 일부 잔존 가능.
     */
    @Deprecated
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
