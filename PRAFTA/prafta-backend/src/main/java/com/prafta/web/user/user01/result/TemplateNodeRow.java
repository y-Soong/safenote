package com.prafta.web.user.user01.result;

/**
 * PRAFTA-049 — 사용자 생성 양식(엑셀) 참조 시트③(소속부서) 1행.
 * 부서코드(NODE_CD)는 사업장 단위로 부여되므로, 식별 편의를 위해 사업장명을 함께 제공한다.
 * (SELECT 컬럼 순서 = 생성자 인자 순서: siteNm, nodeCd, nodeNm)
 */
public record TemplateNodeRow(
    String siteNm
    , String nodeCd
    , String nodeNm
){
}
