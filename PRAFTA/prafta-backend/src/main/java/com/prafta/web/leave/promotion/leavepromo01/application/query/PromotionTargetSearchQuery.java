package com.prafta.web.leave.promotion.leavepromo01.application.query;

import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionTargetSearchParam;

/**
 * prafta-com-008-A-4: 2차 대상자 조회 매퍼 파라미터(스코프 강제값 + 필터).
 *
 * <p>gvCmpnyCd/siteCd 는 토큰 강제(IDOR), nodeCd/incSubNodeYn 은 노드 cascade, userNm/tenureFilter 는
 * 검색 조건. todayYmd 는 1년차(HIRE_DATE) 경계 판정 기준(서비스가 LocalDate.now 로 1회 산출 — 결정성).
 */
public record PromotionTargetSearchQuery(
        String gvCmpnyCd,
        String siteCd,
        String nodeCd,
        String incSubNodeYn,
        String userNm,
        String tenureFilter,
        String oneYearAgoYmd
) {
    /**
     * @param oneYearAgoYmd today 에서 1년 전(YYYYMMDD). HIRE_DATE &lt;= oneYearAgoYmd 면 1년차 이상.
     */
    public static PromotionTargetSearchQuery from(PromotionTargetSearchParam p, String oneYearAgoYmd) {
        return new PromotionTargetSearchQuery(
                p.gvCmpnyCd(),
                p.siteCd(),
                p.nodeCd(),
                p.incSubNodeYn(),
                p.userNm(),
                p.tenureFilter(),
                oneYearAgoYmd
        );
    }
}
