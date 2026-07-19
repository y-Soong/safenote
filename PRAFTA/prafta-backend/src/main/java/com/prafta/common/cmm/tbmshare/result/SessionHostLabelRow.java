package com.prafta.common.cmm.tbmshare.result;

/**
 * 세션별 개최사 라벨 1행(PRAFTA-SUBCON-T5 F10 — 배치 조회용).
 *
 * <p>{@code hostCmpnyNm} = 나를 지정한 직상위 회사명(하향 인접 차수 가시성). 목록 화면에서 행마다
 * 라벨 쿼리를 돌리던 N+1 을 세션코드 집합 단위 1회 조회로 대체한다.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record SessionHostLabelRow(
    String sessionCd
    , String hostCmpnyNm
) {
}
