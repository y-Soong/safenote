package com.prafta.common.cmm.dailycontract.result;

/**
 * 계약서 문서 형식 메타 (멀티페이지 지원 T4 — 온디맨드 도출값).
 *
 * <p>DB 컬럼이 아니라 {@code TB_FILE_INFO.FILE_EXT} + PDF 파싱으로 런타임 도출한다(스키마 무변경 §3).
 * MyBatis 매핑 대상이 아니므로 컴포넌트 순서 제약은 없다.
 *
 * @param formatType 'PDF' | 'IMG'
 * @param pageCount  페이지 수(이미지 원본/레거시 PNG 는 1)
 */
public record ContractDocMeta(
    String formatType
    , int pageCount
) {
}
