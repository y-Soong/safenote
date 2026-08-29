package com.prafta.app.admin.employeestatus.application.query;

import java.util.List;

/**
 * PRAFTA-002: 직원 현황(일자) 로스터 조회용 스코프+필터 Query(매퍼 바인딩 전용).
 *
 * <p>스코프({@code AppAdminAttdMapper}의 {@code AdminAttdScopeQuery}와 동일 축 — ATTD_DETAIL=EMPLOYEE_STATUS):
 * <ul>
 *   <li>companyWide=true(master) → 사업장/노드 무필터(회사 내 siteCd 만 필터).</li>
 *   <li>siteFilter!=null(hr) → 사업장 전부서.</li>
 *   <li>useNodeScope=true(노드관리자) → scopedNodeCds(자기노드+자손) IN 필터.</li>
 * </ul>
 * <p>식별자(cmpnyCd/siteCd)는 서비스에서 토큰/해석된 대상 사업장 값만 전달한다(IDOR 차단). keyword 는 이름 부분일치.
 */
public record EmployeeStatusScopeQuery(
      String cmpnyCd
    , String siteCd
    , boolean companyWide
    , boolean useNodeScope
    , List<String> scopedNodeCds
    , String keyword
    , String workYmd
) {
}
