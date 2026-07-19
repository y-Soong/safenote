package com.prafta.common.cmm.tbmshare.result;

/**
 * 연동 회사 지정 현황 1행(PRAFTA-SUBCON-T5).
 *
 * <p>조회자가 <b>직접 지정한 회사</b>만 반환한다(개설사면 1차 회사, 체인 회사면 자기 재지정분).
 * {@code subCount} 는 그 회사가 다시 지정한 하위 회사 수(개사)로, 하위 회사명/코드는 노출하지 않는다.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record SessionShareRow(
    long shareId
    , String cmpnyCd
    , String cmpnyNm
    , String designatedDtime
    , int subCount
) {
}
