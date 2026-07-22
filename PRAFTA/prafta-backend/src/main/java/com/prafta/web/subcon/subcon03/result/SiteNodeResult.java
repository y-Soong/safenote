package com.prafta.web.subcon.subcon03.result;

/**
 * 사업장 노드(부서) 1건 — 코드 + 명(하도급 부분공유 PS-03).
 *
 * <p>행 단위 마감 커버리지 판정의 노드 유효성(고아 여부) 판정과 제외 부서명 표기에 사용한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SiteNodeResult(
    String nodeCd
    , String nodeNm
){
}
