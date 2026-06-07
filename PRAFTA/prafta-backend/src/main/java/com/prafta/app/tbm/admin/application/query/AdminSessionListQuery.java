package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * T-A1 교육관리 리스트 조회 Query(스코프 적용 포함).
 *
 * <p>[권한매트릭스 §2 / plan §6·T7] 스코프 적용 지점:
 *   <ul>
 *     <li>master/safe(companyWide=true) → 전사(사업장/노드 필터 없음).</li>
 *     <li>노드관리자(companyWide=false) → SITE_CD IN (accessibleSiteCds)
 *         AND MANAGER_USER_CD IN (NODE_CD ∈ scopedNodeCds 인 사용자).</li>
 *   </ul>
 *
 * <p>scopedNodeCds 가 비어 있으면(노드관리자 자손 노드 없음) 노드 필터가 0건을 강제하여 안전하게 차단된다.
 */
public record AdminSessionListQuery(
    String statusCd
    , String keyword
    , boolean companyWide
    , List<String> accessibleSiteCds    // companyWide=false 일 때만 사용(노드관리자 접근 사업장)
    , List<String> scopedNodeCds        // companyWide=false 일 때만 사용(자기노드+자손)
    , int offset
    , int pageSize
    , String gvCmpnyCd
){
}
