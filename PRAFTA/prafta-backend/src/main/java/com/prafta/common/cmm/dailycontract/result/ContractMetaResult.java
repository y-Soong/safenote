package com.prafta.common.cmm.dailycontract.result;

/**
 * 활성 계약서 메타 + 문서 형식 (앱 pager 초기화 — GET /appApi/dailycontract01/contract-meta).
 *
 * <p>파일코드/경로는 포함하지 않는다(열람은 스트림 EP 전용 — 경로 미노출).
 * MyBatis 매핑 대상이 아니므로 컴포넌트 순서 제약은 없다.
 */
public record ContractMetaResult(
    int contractVer
    , String contractNm
    , String formatType    // 'PDF' | 'IMG'
    , int pageCount
) {
}
