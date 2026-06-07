package com.prafta.web.user.user01.result;

/**
 * PRAFTA-049 — 사용자 생성 양식(엑셀) 참조 시트④(권한) 1행.
 * 권한 코드/명은 회사 코드그룹 BAIM_VAL 'COM005' 에서 조회한다.
 * (SELECT 컬럼 순서 = 생성자 인자 순서: authCd, authNm)
 */
public record TemplateAuthRow(
    String authCd
    , String authNm
){
}
