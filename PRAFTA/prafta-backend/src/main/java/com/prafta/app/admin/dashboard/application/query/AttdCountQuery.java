package com.prafta.app.admin.dashboard.application.query;

import java.util.List;

/**
 * J1-10 (B-5): 근태 카운트(출근실제/출근예정/연차) 노드 스코프 Query.
 *
 * <p>스코프 규칙(J1-5 scopeWhere 동형):
 * <ul>
 *   <li>companyWide(master/hr): 회사+사업장만(노드 무필터). useNodeScope=false.</li>
 *   <li>useNodeScope(노드관리자): 노드축 컬럼 IN scopedNodeCds(서비스가 비어있지 않음 보장).</li>
 * </ul>
 * 식별자(cmpnyCd/siteCd)는 token 출처(IDOR 차단). todayYmd 는 DB NOW() 기준 서버 산출.
 */
public record AttdCountQuery(
      String cmpnyCd
    , String siteCd
    , String todayYmd
    , boolean useNodeScope
    , List<String> scopedNodeCds
) {
}
