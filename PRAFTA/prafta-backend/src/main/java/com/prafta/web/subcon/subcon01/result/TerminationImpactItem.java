package com.prafta.web.subcon.subcon01.result;

/**
 * 해지 영향 요약 1건(해지 확인 팝업 표시용 — plan §8).
 *
 * <p>T2(사업장 링크 독립화)/T3(공유요청 자동취소)/T5(TBM 지정 회수) 핸들러가
 * summarize() 로 반환한다. T1 시점엔 구현체가 없어 항상 빈 목록.
 */
public record TerminationImpactItem(
    String impactType   // 영향 유형 식별자(예: SITE_LINK / SHARE_REQ / TBM_SHARE)
    , String label      // 표시 문구(예: "사업장 링크 독립화 예정")
    , int count         // 영향 건수
){
}
