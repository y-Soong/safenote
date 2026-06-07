package com.prafta.app.admin.common.scope.application.query;

/**
 * 001-P1-B2: 노드 스코프 전개(자기노드 + 자손) 공용 Query.
 *
 * <p>[권한매트릭스 §2] 노드관리자 데이터 스코프(자기노드+자손)를 Phase 2~8 목록/집계 쿼리가 공유하기 위한
 * 선행 자산이다. 식별자(cmpnyCd/siteCd/userCd)는 token 출처(IDOR 차단).
 */
public record ScopedNodeQuery(
    String cmpnyCd
    , String siteCd
    , String userCd
) {
    public static ScopedNodeQuery of(String cmpnyCd, String siteCd, String userCd) {
        return new ScopedNodeQuery(cmpnyCd, siteCd, userCd);
    }
}
