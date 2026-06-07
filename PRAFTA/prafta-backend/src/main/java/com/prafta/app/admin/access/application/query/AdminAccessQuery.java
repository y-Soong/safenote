package com.prafta.app.admin.access.application.query;

/**
 * 001-P1-B1: 진입판정 SQL 공용 Query(노드관리자 EXISTS / 접근가능 사업장 조회).
 *
 * <p>식별자는 모두 token 출처(서비스 레이어에서 주입). siteCd 는 현재 기준 사업장이며,
 * 노드관리자 사업장별 EXISTS 판정 및 접근권한 검증에 사용한다.
 */
public record AdminAccessQuery(
    String cmpnyCd
    , String userCd
    , String siteCd
) {
    /** 전사(사업장 무관) 판정용 — siteCd 미사용. */
    public static AdminAccessQuery ofCompany(String cmpnyCd, String userCd) {
        return new AdminAccessQuery(cmpnyCd, userCd, null);
    }

    /** 사업장 기준 판정용. */
    public static AdminAccessQuery ofSite(String cmpnyCd, String userCd, String siteCd) {
        return new AdminAccessQuery(cmpnyCd, userCd, siteCd);
    }
}
