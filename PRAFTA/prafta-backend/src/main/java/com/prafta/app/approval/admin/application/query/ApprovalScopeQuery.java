package com.prafta.app.approval.admin.application.query;

import java.util.List;

/**
 * 001-P2: 근태보정/초과 대기·이력 조회용 스코프+필터 Query(매퍼 바인딩 전용).
 *
 * <p>스코프([권한매트릭스 §2/§4]):
 * <ul>
 *   <li>companyWide=true(master) → 사업장/노드 무필터.</li>
 *   <li>siteFilter!=null(hr) → R.SITE_CD = siteFilter(사업장 전부서).</li>
 *   <li>useNodeScope=true(노드관리자) → R.SITE_CD = nodeSiteCd AND COALESCE(R.NODE_CD, 요청자 U.NODE_CD) ∈ scopedNodeCds.</li>
 * </ul>
 * <p>식별자(cmpnyCd 등)는 서비스에서 토큰값만 전달한다(IDOR 차단).
 */
public record ApprovalScopeQuery(
      String cmpnyCd
    , List<String> reqTypes
    , List<String> reqStatuses
    , boolean companyWide
    , String siteFilter
    , boolean useNodeScope
    , String nodeSiteCd
    , List<String> scopedNodeCds
    , String keyword
    , String startYmd
    , String endYmd
    , int offset
    , int limit
) {
}
