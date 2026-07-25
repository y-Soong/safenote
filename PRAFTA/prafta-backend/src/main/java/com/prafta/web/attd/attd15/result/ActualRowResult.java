package com.prafta.web.attd.attd15.result;

/**
 * ATTD15-T1 - "실제 근무 기준" 원시(raw) 출퇴근 행.
 *
 * <p>한 행 = (USER_CD, WORK_YMD, WORK_SEQ) 단위 출퇴근 실적 + 해당 차수의 스케줄 휴게분.
 * {@code Attd11Mapper.selectAttdSummaryRows} 를 주간 범위(WORK_YMD BETWEEN)로 이식한 것으로,
 * 사용자 결정(§2.1 채택안)에 따라 "실제 근무"는 Attd_11 방식(원시 근로시간, 스케줄 클램프 없음)을
 * 그대로 따른다 — 서비스에서 {@code Attd11ServiceImpl.UserAccumulator.workMinutes} 와 동일한
 * 계산식((퇴근일시-출근일시)-스케줄휴게, 음수 0 클램프)을 포팅해 사용자별로 주간 합산한다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record ActualRowResult(
        String userCd
        , String workYmd          // YYYYMMDD
        , int workSeq              // 1 | 2

        /* 차수에 맞춰 정규화한 스케줄 휴게분(공제용, 분 문자열) */
        , String planBreakMin

        /* 실제(원본) 출퇴근 일시 — 표준화 아님 */
        , String actInDate        // YYYYMMDD, nullable
        , String actInTime        // HHmm, nullable
        , String actOutDate       // YYYYMMDD, nullable
        , String actOutTime       // HHmm, nullable
) {
}
