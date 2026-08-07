package com.prafta.web.attd.attd11.result;

/**
 * HB-05(D1)/D-3: 조회 월의 확정 반차('01' + 시각 보유) 면제 구간 1건.
 *
 * <p>한 날에 여러 건(시작기준 + 종료기준)이 있을 수 있어 집계 없이 행 단위로 받는다.
 * 합집합 판정은 {@code PartialLeaveWindowUtils.resolveAll} 이 담당한다.
 * (모듈 간 매퍼 직접 의존을 피하려고 Attd_08 의 동명 record 와 구조만 동일한 미러다.)
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Attd11Mapper.selectHalfLeaveWindows} 와 순서 일치.
 */
public record HalfLeaveWindowResult(
      String userCd
    , String workYmd
    , String startTime
    , String endTime
) {
}
