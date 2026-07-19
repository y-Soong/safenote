package com.prafta.common.cmm.tbmshare.result;

/**
 * 지정 체인 1행(PRAFTA-SUBCON-T5) — 개설사에서 하향 도달 가능한 회사.
 *
 * <p>{@code tier1CmpnyCd}/{@code tier1CmpnyNm} 는 개설사 직하 1차 회사(= 개설사 화면 relabel 대상).
 * 2차 이하 회사는 자기를 낳은 1차 회사의 라벨을 물려받는다(마스터 §1-3 인접 차수 가시성).
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record ShareChainRow(
    String shareCmpnyCd
    , String tier1CmpnyCd
    , String tier1CmpnyNm
    , int depth
) {
}
