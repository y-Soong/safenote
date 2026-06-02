package com.prafta.app.attd.attd01.application.query;

/**
 * prafta-app-002: 본인 근태 조회 공통 Query (mapper 진입).
 *
 * <p>모든 SELECT 가 본인 스코프(cmpnyCd/siteCd/userCd) + 일자 범위(fromYmd~toYmd, 포함) 로 한정된다.
 *   단일 일자 조회(today/day-detail)는 fromYmd==toYmd 로 호출한다.
 *   cmpnyCd/siteCd/userCd 는 Param 에서 JWT 출처로 캐노니컬라이즈된 값만 들어온다(IDOR 가드).
 */
public record AttdRangeQuery(
    String cmpnyCd
    , String siteCd
    , String userCd
    , String fromYmd
    , String toYmd
) {
}
