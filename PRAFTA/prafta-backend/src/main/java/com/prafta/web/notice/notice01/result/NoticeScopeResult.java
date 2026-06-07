package com.prafta.web.notice.notice01.result;

/**
 * 발행자 대상선택 트리의 노드 1건(사업장 또는 노드).
 * level='SITE' 면 사업장 행(nodeCd null), level='NODE' 면 노드 행.
 * 프론트는 siteCd/nodeCd/parentNodeCd 로 트리를 구성한다.
 */
public record NoticeScopeResult(
    String level
    , String siteCd
    , String siteNm
    , String nodeCd
    , String nodeNm
    , String parentNodeCd
){
}
