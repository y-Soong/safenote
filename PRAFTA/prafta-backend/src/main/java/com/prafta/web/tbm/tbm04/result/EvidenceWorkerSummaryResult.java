package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 근로자별 반기 이수 집계 1행 (시트2 "근로자별 이수현황").
 *
 * <p>모수 = 자사 활성 정규직(사업장 필터 적용) 전원 + 반기 중 이수 기록이 있는 자사 일용직.
 * 시간 축은 확정안대로 <b>세션 인정시간(EDU_MINUTES)</b>이며 이수(COMPLETED) 건만 합산한다.
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceWorkerSummaryResult(
    String userTypeCd          // REGULAR | DAILY
    , String userCd
    , String userNm
    , Integer tbmCount         // 반기 이수 세션 수
    , Integer tbmMinutes       // 반기 인정시간 합(분)
){
}
