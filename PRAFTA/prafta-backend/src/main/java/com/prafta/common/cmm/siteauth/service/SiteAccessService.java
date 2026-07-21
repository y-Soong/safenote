package com.prafta.common.cmm.siteauth.service;

/**
 * 사업장 접근 인가 공용 서비스.
 *
 * <p>기존 param 단계의 "요청 siteCd == 토큰 gv_siteCd" 등식 강제는 User_03 사업장 권한
 * 원장(TB_USER_SITE_AUTH)과 불일치해 다사업장 운영(하도급 미러 사업장 포함)을 막았다.
 * 본 서비스가 사업장 스코프 인가의 단일 기준이다.
 *
 * <p>판정 규칙(순서대로 하나라도 충족 시 허용):
 * <ol>
 *   <li>master / hr — 전 사업장 접근 허용(전사 관리 역할).</li>
 *   <li>요청 사업장 == 토큰 사업장(gv_siteCd) — 자기 사업장 fast path(DB 미조회).</li>
 *   <li>TB_USER_SITE_AUTH(USE_YN='Y') 보유 — User_03 에서 부여한 사업장 권한.</li>
 * </ol>
 *
 * <p>⚠️ 본 판정은 대상 사업장의 "자사 소속/실재"를 보장하지 않는다(특히 master/hr 전사 통과 경로).
 * 소비처 SQL 은 반드시 CMPNY_CD = 토큰 gv_cmpnyCd 스코프를 함께 유지해야 한다 — assert 통과를
 * "자사 유효 사업장" 검증으로 오해하지 말 것.
 */
public interface SiteAccessService {

    /** 사업장 접근 가능 여부. targetSiteCd 가 비면 false(호출부에서 필수값 검증 선행). */
    boolean hasSiteAccess(String gvCmpnyCd, String gvUserCd, String gvAuthCd, String gvSiteCd, String targetSiteCd);

    /** 접근 불가 시 {@code COMMON_403_003} 을 던진다. */
    void assertSiteAccess(String gvCmpnyCd, String gvUserCd, String gvAuthCd, String gvSiteCd, String targetSiteCd);
}
