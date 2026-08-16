package com.prafta.app.admin.access.application.helper;

import java.util.LinkedHashMap;
import java.util.Map;

import com.prafta.app.admin.access.dto.response.AdminRoleAxisResponse;
import com.prafta.common.util.AuthRoleUtils;

/**
 * 001-P1-B1/B2: 관리자 모드 권한 매트릭스 산출 단일 출처.
 *
 * <p>[권한매트릭스 §3] 모듈×역할 활성식과 §2 데이터 스코프 규칙을 본 클래스 1곳에만 구현한다.
 * 매트릭스 cell 이 바뀌면 이 클래스만 수정한다(서비스/매퍼/프론트 무변경).
 *
 * <p>도메인(관리자 모드) 종속 규칙이므로 {@code common.util} 가 아니라 admin 모듈 내부에 둔다
 * (developer 정책: common.util 에는 도메인 비종속 유틸만 배치).
 *
 * <p>역할코드 판정은 {@link AuthRoleUtils} 상수를 단일 출처로 사용한다(DB 추가조회 없음).
 */
public final class AdminAccessResolver {

    // 모듈 키([권한매트릭스 §3] IA 모듈) — 프론트 grid key 와 1:1.
    public static final String MODULE_HOME = "HOME";
    public static final String MODULE_APPROVAL = "APPROVAL";
    public static final String MODULE_ATTD_DETAIL = "ATTD_DETAIL";
    public static final String MODULE_SAFETY = "SAFETY";
    public static final String MODULE_TBM = "TBM";
    public static final String MODULE_SITE_OPS = "SITE_OPS";
    public static final String MODULE_BOARD = "BOARD";
    public static final String MODULE_SETTINGS = "SETTINGS";
    /** 일용직 입장 승인(일용직 계약서+승인제 T4). 서버 최종 강제는 DailyEntryService.assertSiteAuthority. */
    public static final String MODULE_ENTRY = "ENTRY";
    /** 셀프가입(회원가입) 승인. 서버 최종 강제는 User09Service 의 2단 게이트. */
    public static final String MODULE_SELF_JOIN = "SELF_JOIN";

    private AdminAccessResolver() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 역할코드 축 산출(authCd → master/hr/safe boolean).
     */
    public static AdminRoleAxisResponse buildRoleAxis(String authCd) {
        boolean isMaster = AuthRoleUtils.AUTH_MASTER.equals(authCd);
        boolean isHr = AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd);
        boolean isSafe = AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd);

        return AdminRoleAxisResponse.builder()
                .authCd(authCd)
                .master(isMaster)
                .hr(isHr)
                .safe(isSafe)
                .build();
    }

    /**
     * 진입 가능 여부 = (master|hr|safe) OR 노드관리자(전사 기준).
     * <p>[권한매트릭스 §1] 미출근 상태와 무관. roleAxis 또는 어느 사업장이든 노드 정/부 관리자면 진입 허용.
     *
     * @param isNodeAdminAnySite 전사(사업장 무관) 노드 정/부 관리자 존재 여부
     */
    public static boolean canEnterAdmin(AdminRoleAxisResponse axis, boolean isNodeAdminAnySite) {
        return axis.isMaster() || axis.isHr() || axis.isSafe() || isNodeAdminAnySite;
    }

    /**
     * 모듈 활성식 산출([권한매트릭스 §3], plan 2-3 표).
     * <ul>
     *   <li>HOME/SITE_OPS : master ∥ hr ∥ safe ∥ (현재 사업장)nodeAdmin.
     *       전사역할 보유자이거나 현재 선택 사업장의 노드관리자일 때만 활성.
     *       단순 진입가능(canEnterAdmin)만으로 열지 않는다 — 타 사업장 전용 노드관리자가
     *       권한 없는 현재 사업장에서 전사 스코프로 열리는 권한상승/스코프 누수를 차단한다.</li>
     *   <li>APPROVAL/ATTD_DETAIL : master ∥ hr ∥ nodeAdmin (safe 단독 ⛔).</li>
     *   <li>SAFETY/TBM : master ∥ safe ∥ nodeAdmin (hr 동시보유는 합집합으로 활성 — §3.2 확정).</li>
     *   <li>BOARD/SETTINGS : 데이터 스코프 없는 모듈이므로 항상 활성(BOARD 는 web 미구현=준비중).</li>
     *   <li>ENTRY : master ∥ hr (일용직 입장 승인 — 일용직 계약서+승인제 T4).
     *       서버 EP(entryadmin01)의 최종 인가(DailyEntryService.assertSiteAuthority = master/hr)와
     *       동일 기준. nodeAdmin 단독은 서버가 403 이므로 여기서도 열지 않는다(fail-closed 정합).</li>
     *   <li>SELF_JOIN : master ∥ hr ∥ nodeAdmin (셀프가입 승인 — safe 단독 ⛔).
     *       서버 EP(User09Service)의 최종 인가(canManageNodeExcludeSafe = master/hr 전사 통과 +
     *       해당·상위 부서 정/부 관리자)와 정확히 같은 집합이라 APPROVAL 활성식을 재사용한다.</li>
     * </ul>
     *
     * @param isNodeAdminInSite 현재 선택 사업장 기준 노드 정/부 관리자 여부
     */
    public static Map<String, Boolean> buildModuleActiveMap(AdminRoleAxisResponse axis, boolean isNodeAdminInSite) {
        // HOME/SITE_OPS 도 다른 데이터 스코프 모듈과 동일하게 현재 사업장 권한 기준으로 게이트한다.
        boolean homeActive = axis.isMaster() || axis.isHr() || axis.isSafe() || isNodeAdminInSite;
        boolean siteOpsActive = axis.isMaster() || axis.isHr() || axis.isSafe() || isNodeAdminInSite;
        boolean approvalActive = axis.isMaster() || axis.isHr() || isNodeAdminInSite;
        boolean safetyActive = axis.isMaster() || axis.isSafe() || isNodeAdminInSite;
        // 일용직 입장 승인 — 서버 최종 인가(master/hr)와 동일 기준(nodeAdmin 제외, fail-closed).
        boolean entryActive = axis.isMaster() || axis.isHr();

        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(MODULE_HOME, homeActive);
        map.put(MODULE_APPROVAL, approvalActive);
        map.put(MODULE_ATTD_DETAIL, approvalActive);
        map.put(MODULE_ENTRY, entryActive);
        // 셀프가입 승인 — 서버 게이트(canManageNodeExcludeSafe)와 동일 집합이라 approvalActive 재사용.
        map.put(MODULE_SELF_JOIN, approvalActive);
        map.put(MODULE_SAFETY, safetyActive);
        map.put(MODULE_TBM, safetyActive);
        map.put(MODULE_SITE_OPS, siteOpsActive);
        map.put(MODULE_BOARD, true);
        map.put(MODULE_SETTINGS, true);
        return map;
    }

    /**
     * 데이터 스코프(🔵) 적용 모듈 산출([권한매트릭스 §2/§3.2]).
     * <p>해당 모듈이 "노드관리자 축으로만" 열린 경우 true(자기노드/자손으로 제한). 동시보유로
     * 전사 역할(master/hr/safe)이 같은 모듈을 열면 전사 우월 → scoped=false.
     * <p>⚠️ scoped=false 의 의미는 <b>active=true 인 모듈에 한해 "전사 우월(master/hr/safe 보유)"</b>
     * 을 뜻한다. active=false(권한 없어 노드범위가 안 잡힌 상태)인 모듈은 scoped 가 false 로 산출되더라도
     * 무의미하다(카드 자체가 비활성). 즉 "전사 권한 보유"와 "권한 없음"을 scoped=false 하나로 혼동하지 않도록,
     * 반드시 active 플래그와 함께 해석한다.
     *
     * @param isNodeAdminInSite 현재 선택 사업장 기준 노드 정/부 관리자 여부
     */
    public static Map<String, Boolean> buildModuleScopedMap(AdminRoleAxisResponse axis, boolean isNodeAdminInSite) {
        // 전사 역할 보유 여부(모듈별 전사 우월 판정 입력).
        boolean anyCompanyWide = axis.isMaster() || axis.isHr() || axis.isSafe();        // HOME/SITE_OPS 는 전 역할이 전사
        boolean approvalCompanyWide = axis.isMaster() || axis.isHr();                    // 승인/근태 전사 역할
        boolean safetyCompanyWide = axis.isMaster() || axis.isSafe();                    // 안전/TBM 전사 역할

        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(MODULE_HOME, isNodeAdminInSite && !anyCompanyWide);
        map.put(MODULE_APPROVAL, isNodeAdminInSite && !approvalCompanyWide);
        map.put(MODULE_ATTD_DETAIL, isNodeAdminInSite && !approvalCompanyWide);
        map.put(MODULE_SELF_JOIN, isNodeAdminInSite && !approvalCompanyWide);
        map.put(MODULE_SAFETY, isNodeAdminInSite && !safetyCompanyWide);
        map.put(MODULE_TBM, isNodeAdminInSite && !safetyCompanyWide);
        map.put(MODULE_SITE_OPS, isNodeAdminInSite && !anyCompanyWide);
        return map;
    }
}
