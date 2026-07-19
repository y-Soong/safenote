package com.prafta.app.admin.access.dto.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prafta.app.admin.access.result.AccessibleSiteResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P1-B1: 관리자 모드 진입판정(access-context) 응답.
 *
 * <p>경로: GET /appApi/admin/access-context (선택 siteCd 는 현장전환 재조회용, 서버가 USE_YN='Y' 검증 — D5).
 * <p>식별자(cmpnyCd/userCd/siteCd/authCd)는 전부 JWT 클레임에서만 도출한다(IDOR 차단, D1).
 *
 * <p>활성/스코프 산출은 {@code AdminAccessResolver} 1곳([권한매트릭스 §3])에서만 한다.
 * 클라이언트는 본 맵을 렌더만 하며 역할(AUTH_CD) 기반 분기를 두지 않는다(C1).
 *
 * <p>JSON 키는 plan §3-0 계약에 맞춰 고정한다(Jackson boolean is-prefix 변형 방지를 위해 @JsonProperty 명시).
 */
@Getter
@Builder
public class AdminAccessContextResponse {

    /** 관리자 모드 진입 가능 여부 = (master|hr|safe) OR 노드관리자(전사). */
    @JsonProperty("canEnterAdmin")
    private final boolean canEnterAdmin;

    /** 역할코드 축(authCd/isMaster/isHr/isSafe). */
    private final AdminRoleAxisResponse roleAxis;

    /** 노드관계 축 — 현재 선택 사업장 기준 정/부 관리자 여부(EXISTS). moduleActiveMap 산출 입력. */
    @JsonProperty("isNodeAdmin")
    private final boolean nodeAdmin;

    /** 현재 선택 사업장코드(선택 siteCd 검증값 또는 토큰 gv_siteCd). */
    private final String currentSiteCd;

    /** 현장 전환 셀렉터 노출 여부 = accessibleSites 수 > 1. */
    @JsonProperty("siteSwitchEnabled")
    private final boolean siteSwitchEnabled;

    /** USE_YN='Y' 접근 가능 사업장 목록(현장전환 셀렉터용). */
    private final List<AccessibleSiteResult> accessibleSites;

    /** 모듈 활성식 산출 맵([권한매트릭스 §3]). HOME/APPROVAL/ATTD_DETAIL/ENTRY/SAFETY/TBM/SITE_OPS/BOARD/SETTINGS. */
    private final Map<String, Boolean> moduleActiveMap;

    /** 데이터 스코프(🔵 노드관리자 단독 축) 적용 모듈 맵(UI 배지용). */
    private final Map<String, Boolean> moduleScopedMap;
}
