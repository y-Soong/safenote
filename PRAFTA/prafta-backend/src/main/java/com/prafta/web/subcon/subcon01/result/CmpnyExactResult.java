package com.prafta.web.subcon.subcon01.result;

/**
 * 회사 정확일치 조회 결과 1행 — 응답 3필드 한정(§6-1: 주소/우편번호/계약정보/감사컬럼 반환 금지).
 *
 * <p>record 매핑은 SELECT 컬럼 순서와 일치해야 한다(MyBatis record 컬럼순서 규약).
 */
public record CmpnyExactResult(
    String cmpnyCd
    , String cmpnyNm
    , String bsnsLcnNo
){
}
