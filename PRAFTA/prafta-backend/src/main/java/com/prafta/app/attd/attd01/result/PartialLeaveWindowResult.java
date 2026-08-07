package com.prafta.app.attd.attd01.result;

/**
 * HB-08(D5) 앱 미러 - 그날 확정 부분연차(반차 '01' + 시간차 '02'~'04') 1건의 면제 구간.
 *
 * <p>앱 초과근무 발의 가드가 "연차로 쉬는 시간대에 OT 를 올리는 것"을 거부(ATTD_400_196)하는 데 쓴다.
 * 웹 {@code Attd07Mapper.selectLeaveExemptWindows} 와 동일 술어의 미러다.
 *
 * <p>★ Q5 정정(2026-08-07): 연차 행은 {@code START_DATE = END_DATE = 근무일} 고정이다.
 * 자정 넘김은 {@code END_TIME < START_TIME} 이면 종료가 익일이라는 <b>시각 wrap</b> 으로만 표현되며,
 * 절대 시각 환산은 단일 진입점 {@code PartialLeaveWindowUtils.exemptStampRange}
 * (그날 원 스케줄을 프레임으로 정렬)가 담당한다 — 호출부는 프레임만 넘긴다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record PartialLeaveWindowResult(
        String startDate     // YYYYMMDD
        , String startTime   // HHmm
        , String endDate     // YYYYMMDD
        , String endTime     // HHmm
) {
}
