package com.prafta.web.attd.attd07.result;

/**
 * HB-08(D5) - 그날 확정 부분연차(반차 '01' + 시간차 '02'~'04') 1건의 면제 구간.
 *
 * <p>초과근무 등록 가능 범위 산정에서 <b>차집합의 피감수(subtrahend)</b>로 스케줄 구간과 합집합된다:
 * <pre>등록 가능 범위 = 실근태 - ( 스케줄 구간 ∪ 연차 면제 구간 )</pre>
 *
 * <p>시각은 HHmm(varchar(4)), 일자는 YYYYMMDD(varchar(8)).
 * ★ Q5 정정(2026-08-07): 연차 행은 {@code START_DATE = END_DATE = 근무일} 고정이며 자정 넘김은
 * {@code END_TIME < START_TIME} 시각 wrap 으로 표현된다. 분 stamp 변환은 단일 진입점
 * {@code PartialLeaveWindowUtils.exemptStampRange}(workYmd-1 00:00 = 0 기준, <b>그날 원 스케줄 프레임 필수</b>)를
 * 쓴다 — {@code startDate}/{@code endDate} 는 항상 근무일이라 환산 입력이 아니고 로그/방어용이다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record LeaveExemptWindowResult(
        String startDate     // YYYYMMDD
        , String startTime   // HHmm
        , String endDate     // YYYYMMDD
        , String endTime     // HHmm
) {
}
