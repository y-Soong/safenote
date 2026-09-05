package com.prafta.web.attd.attd08.result;

/**
 * HB-05(D1): 조회기간 안의 확정 반차('01' + 시각 보유) 면제 구간 1건.
 *
 * <p>한 날에 여러 건(시작기준 + 종료기준)이 있을 수 있어 집계 없이 행 단위로 받는다.
 * 합집합 판정은 {@code PartialLeaveWindowUtils.resolveAll} 이 담당한다(D-3).
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Attd08Mapper.selectHalfLeaveWindows} 와 순서 일치.
 */
public record HalfLeaveWindowResult(
      String userCd
    , String workYmd
    , String startTime
    , String endTime
    /** BW-05: 사용단위(01 반차 / 02~04 시간차). ★record 끝 — 위치매핑(두 select 모두 동수 추가). */
    , String useUnitType
    /** BW-05: 휴게 무시 요청 Y/N — 반차(01)+Y 인 날은 FE 인정시간/실근로에서 스케줄 휴게 공제 0. */
    , String brkWaiveYn
    /** v2(BW2-13): 넘긴 휴게 분량(반차 W / 시간차 편입분 / 기록 전용 0). NULL + Y = v1 전부. ★record 끝(두 select 동시). */
    , Integer brkWaiveMin
) {
}
