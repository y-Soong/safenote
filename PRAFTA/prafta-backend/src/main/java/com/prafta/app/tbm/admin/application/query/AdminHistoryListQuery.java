package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * R6 이력 리스트/통계 조회 Query(스코프 적용 포함).
 *
 * <p>R3 리스트와 동일 스코프 — companyWide(master/safe)=전사. 노드관리자=접근가능 사업장
 *   AND 개설자 노드(NODE_CD ∈ scopedNodeCds). statusCd 미지정 시 COMPLETED/CANCELLED.
 */
public record AdminHistoryListQuery(
    String statusCd
    , String startDate
    , String endDate
    , String keyword
    , boolean companyWide
    , List<String> accessibleSiteCds
    , List<String> scopedNodeCds
    , int offset
    , int pageSize
    , String gvCmpnyCd
){
}
