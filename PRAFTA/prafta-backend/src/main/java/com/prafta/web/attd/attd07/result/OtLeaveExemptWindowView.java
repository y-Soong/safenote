package com.prafta.web.attd.attd07.result;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * OT 칩 정합(2026-08-08): 일자상세 응답에 싣는 <b>연차 면제 구간</b> 표시용 뷰.
 *
 * <p><b>신설 배경</b> — 일자상세 팝업의 "등록 가능" OT 칩이 FE 자체 계산(실근태−스케줄)이라
 * 연차 면제 구간을 반영하지 못했다. 반차일 2차 재출근 구간에서 칩은 전량(예: 15:00~19:00)을
 * 보여주지만 서버 검증({@code ATTD_400_012})은 면제 겹침분을 거부해 <b>칩과 저장 결과가
 * 불일치</b>했다(무인테스트 실측, 2026-08-08). 산출은 서버 검증과 동일한 단일 출처
 * ({@code Attd07ServiceImpl.buildLeaveExemptSegments} → {@code PartialLeaveWindowUtils})를
 * 재사용하고, FE 는 계산 없이 그대로 뺀다.
 *
 * <p><b>stamp 축</b> — 원점 = workYmd <b>전날</b> 00:00 (= 0). 당일 00:00 = 1440.
 * 웹 FE {@code buildActualSegments} 의 {@code (dayDiff+1)*1440+min} 과 동일 축이라 변환 없이
 * 차집합에 바로 쓸 수 있다. 날짜·시각 필드는 stamp 를 역변환한 등가 표현(앱·표시용).
 *
 * @param startStamp 면제 시작 (분 stamp)
 * @param endStamp   면제 종료 (분 stamp, exclusive)
 * @param startDate  면제 시작 일자 (YYYYMMDD)
 * @param startTime  면제 시작 시각 (HHmm)
 * @param endDate    면제 종료 일자 (YYYYMMDD)
 * @param endTime    면제 종료 시각 (HHmm)
 */
public record OtLeaveExemptWindowView(
      int startStamp
    , int endStamp
    , String startDate
    , String startTime
    , String endDate
    , String endTime
) {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 검증 경로가 쓰는 stamp 구간({@code int[]{start, end}})을 그대로 받아 뷰로 변환한다.
     * 역변환 규칙: dayOffset = stamp/1440 − 1 (0=당일), 시각 = stamp%1440.
     */
    public static OtLeaveExemptWindowView fromStampRange(String workYmd, int[] range) {
        LocalDate base = LocalDate.parse(workYmd, YMD);
        return new OtLeaveExemptWindowView(
                range[0], range[1],
                base.plusDays(range[0] / 1440 - 1).format(YMD), toHhmm(range[0] % 1440),
                base.plusDays(range[1] / 1440 - 1).format(YMD), toHhmm(range[1] % 1440));
    }

    private static String toHhmm(int minOfDay) {
        return String.format("%02d%02d", minOfDay / 60, minOfDay % 60);
    }
}
