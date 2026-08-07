package com.prafta.web.attd.attd11.result;

/**
 * HB-06(2026-08-07) - Attd_11 결근 계상용 일자별 원시 행.
 *
 * <p>한 행 = "스케줄이 배정됐고 · 미래일이 아니며 · 휴일이 아니고 · 종일 연차도 아니며 ·
 * 출근기록이 없는" 근무일 1일. 서비스가 아래 산식으로 결근을 <b>분(minutes)</b>으로 계상한다:
 *
 * <pre>
 *   결근분 = max(0, D - leaveExemptMinutes)      (D = 그날 소정근로분)
 *   결근일수 = 결근분 / D
 * </pre>
 *
 * <p>D 는 SQL 이 아니라 {@code ScheduleWorkMinutesUtils.dailyStdWorkMinutes} 로 계산한다
 * (연차 차감·주52·반차 경계와 <b>동일 산식 단일 출처</b> 유지).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record AbsentDayRowResult(
        String userCd
        , String workYmd            // YYYYMMDD

        /* 1구간 계획시각/휴게 */
        , String fstSchStrTime      // HHmm
        , String fstSchEndTime      // HHmm
        , String fstSchBrkMin       // 분(문자열)

        /* 2구간 계획시각/휴게 (없으면 전부 null) */
        , String secSchStrTime
        , String secSchEndTime
        , String secSchBrkMin

        /** 그날 확정 부분연차(반차 '01' + 시간차 '02'~'04') 면제분 합계(분). 없으면 0. */
        , int leaveExemptMinutes
) {
}
