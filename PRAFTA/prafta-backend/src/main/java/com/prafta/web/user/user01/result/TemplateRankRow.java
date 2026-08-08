package com.prafta.web.user.user01.result;

/**
 * 사용자 생성 양식(엑셀) 참조 시트⑤(직급) 1행.
 * 직급 코드/명은 회사 코드그룹 BAIM_VAL 'COM007' 에서 조회한다.
 * (SELECT 컬럼 순서 = 생성자 인자 순서: rankCd, rankNm)
 */
public record TemplateRankRow(
    String rankCd
    , String rankNm
){
}
