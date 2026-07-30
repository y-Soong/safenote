package com.prafta.web.leave.promotion.leavepromo01.application.query;

import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionFirstTargetSearchParam;

/**
 * 1차 현황 조회 매퍼 파라미터(스코프 강제값 + 필터).
 *
 * <p>gvCmpnyCd/siteCd 는 토큰·인가 통과값(IDOR), nodeCd/incSubNodeYn 은 노드 cascade, userNm 은 검색 조건.
 *
 * <p><b>날짜 파라미터가 없다</b> — 제출 기한(통지일+10일)·2차 도래 예정일(만료-3개월)·촉진 상한
 * (만료-2개월) 등 모든 오프셋 연산은 Java(서비스) 한쪽에서만 수행한다(경계 불일치 방지).
 * SQL 은 표기 변환(DATE_FORMAT)·동등/부등 비교·집계까지만 담당한다.
 */
public record PromotionFirstTargetSearchQuery(
        String gvCmpnyCd,
        String siteCd,
        String nodeCd,
        String incSubNodeYn,
        String userNm
) {
    public static PromotionFirstTargetSearchQuery from(PromotionFirstTargetSearchParam p) {
        return new PromotionFirstTargetSearchQuery(
                p.gvCmpnyCd(),
                p.siteCd(),
                p.nodeCd(),
                p.incSubNodeYn(),
                p.userNm()
        );
    }
}
