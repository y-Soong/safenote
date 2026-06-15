package com.prafta.app.attd.admin.application.query;

import java.util.List;

/**
 * J1-5: 일자/월 근태 조회용 스코프+필터 Query(매퍼 바인딩 전용).
 *
 * <p>스코프([권한매트릭스 §2/§4], 앱 승인관리 ApprovalScopeQuery 패턴 동형):
 * <ul>
 *   <li>companyWide=true(master) → 사업장/노드 무필터(회사 내 siteCd 만 필터).</li>
 *   <li>siteFilter!=null(hr) → 사업장 전부서.</li>
 *   <li>useNodeScope=true(노드관리자) → scopedNodeCds(자기노드+자손) IN 필터.</li>
 * </ul>
 * <p>식별자(cmpnyCd/siteCd)는 서비스에서 토큰값만 전달한다(IDOR 차단). keyword 는 이름 부분일치.
 * <p>daily 는 workYmd 단일일, monthly 는 workYm(YYYYMM)로 LIKE 한다(서비스가 둘 중 하나만 채움).
 */
public record AdminAttdScopeQuery(
      String cmpnyCd
    , String siteCd
    , boolean companyWide
    , boolean useNodeScope
    , List<String> scopedNodeCds
    , String keyword
    , String workYmd        // daily 전용(YYYYMMDD)
    , String workYm         // monthly 전용(YYYYMM)
) {
}
