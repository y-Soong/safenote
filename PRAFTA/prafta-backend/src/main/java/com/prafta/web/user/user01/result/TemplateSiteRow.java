package com.prafta.web.user.user01.result;

/**
 * PRAFTA-049 — 사용자 생성 양식(엑셀) 참조 시트②(사업장) 1행.
 * 일괄 업로드 폼은 '사업장번호(SITE_NO)' 를 입력받으므로 siteNo 를 함께 제공한다.
 * (SELECT 컬럼 순서 = 생성자 인자 순서: siteNm, siteNo, siteCd)
 */
public record TemplateSiteRow(
    String siteNm
    , String siteNo
    , String siteCd
){
}
