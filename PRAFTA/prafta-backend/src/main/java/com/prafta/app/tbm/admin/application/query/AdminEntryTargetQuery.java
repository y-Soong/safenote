package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * E10 대리입실 대상 유효성 검증 Query(prafta-051 R-B, web Tbm02 countEntryTarget 포팅).
 *
 * <p>세션 사업장(siteCd, 서버 확정) 소속 활성 사용자 여부를 재검증한다(IDOR/스코프 누수 차단).
 * R-B 는 정규직(REGULAR)만 대상으로 한다(일용직 QR=MANAGER_QR_SCAN 은 R-D).
 *
 * <p>노드 스코프(E9 selectEligibleRegulars 동형): companyWide=master/safe(노드 필터 없음).
 * 노드관리자는 scopedNodeCds(세션 사업장 기준 자기노드+자손)로 TB_USER.NODE_CD 를 제한한다
 * (읽기/쓰기 권한 비대칭 차단 — 서브트리 밖 정규직 대리입실 거부).
 */
public record AdminEntryTargetQuery(
    String gvCmpnyCd
    , String siteCd
    , String userTypeCd
    , String userCd
    , boolean companyWide
    , List<String> scopedNodeCds
){
    public static AdminEntryTargetQuery of(
            String gvCmpnyCd, String siteCd, String userTypeCd, String userCd,
            boolean companyWide, List<String> scopedNodeCds) {
        return new AdminEntryTargetQuery(
                gvCmpnyCd, siteCd, userTypeCd, userCd, companyWide, scopedNodeCds);
    }
}
