package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * E10/E11 대리입실 대상 유효성 검증 Query(prafta-051 R-B/R-D + PRAFTA-SUBCON-T5).
 *
 * <p>대상이 활성 사용자인지 재검증한다(IDOR/스코프 누수 차단).
 *
 * <p><b>T5</b>: 대상 회사는 {@code targetCmpnyCd}(공통 게이트 {@code assertEntryAllowed} 통과값).
 * {@code ownTarget}(= 대상이 개설사 자신) 일 때만 사업장/노드 조건을 건다.
 * 타사 대상은 회사 단위 지정이라 사업장 조건을 걸지 않으며(plan D4), 타사 부서 트리를 알 수 없으므로
 * 노드 스코프도 적용하지 않는다(적용하면 항상 0건 → 기능 불능. plan D7).
 */
public record AdminEntryTargetQuery(
    String targetCmpnyCd
    , String siteCd
    , String userTypeCd
    , String userCd
    , boolean companyWide
    , List<String> scopedNodeCds
    , boolean ownTarget
){
    public static AdminEntryTargetQuery of(
            String targetCmpnyCd, String siteCd, String userTypeCd, String userCd,
            boolean companyWide, List<String> scopedNodeCds, boolean ownTarget) {
        return new AdminEntryTargetQuery(
                targetCmpnyCd, siteCd, userTypeCd, userCd, companyWide, scopedNodeCds, ownTarget);
    }
}
