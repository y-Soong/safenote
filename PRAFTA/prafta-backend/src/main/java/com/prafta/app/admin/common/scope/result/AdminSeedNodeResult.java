package com.prafta.app.admin.common.scope.result;

/**
 * 관리자 데이터 스코프 — 내가 정/부 관리자인 노드(seed) 1건.
 *
 * <p>앱 화면의 부서 필터 칩 소스다. 자손 노드는 포함하지 않는다(조회 시 서버가
 * {@code incSubNodeYn='Y'} 로 전개한다).
 *
 * <p>★record 는 위치 매핑이다 — 필드 순서 = SELECT 컬럼 순서(nodeCd, nodeNm)를 반드시 유지할 것.
 */
public record AdminSeedNodeResult(
        String nodeCd
        , String nodeNm
) {
}
