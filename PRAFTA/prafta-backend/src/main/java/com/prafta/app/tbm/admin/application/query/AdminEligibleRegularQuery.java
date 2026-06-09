package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * E9 정규직 대리입실 후보 검색 Query(prafta-051 R-B).
 *
 * <p>siteCd 는 세션 사업장(guard.siteCd, 서버 확정). companyWide=master/safe(노드 필터 없음).
 * 노드관리자는 scopedNodeCds(세션 사업장 기준 자기노드+자손)로 TB_USER.NODE_CD 를 제한한다(스코프 격리).
 * 식별자(회사)는 토큰 출처. offset/pageSize 로 LIMIT 페이징.
 */
public record AdminEligibleRegularQuery(
    String sessionCd
    , String gvCmpnyCd
    , String siteCd
    , String keyword
    , boolean companyWide
    , List<String> scopedNodeCds
    , int offset
    , int pageSize
){
    public static AdminEligibleRegularQuery of(
            String sessionCd, String gvCmpnyCd, String siteCd, String keyword,
            boolean companyWide, List<String> scopedNodeCds, int offset, int pageSize) {

        return new AdminEligibleRegularQuery(
            sessionCd, gvCmpnyCd, siteCd, keyword, companyWide, scopedNodeCds, offset, pageSize);
    }
}
