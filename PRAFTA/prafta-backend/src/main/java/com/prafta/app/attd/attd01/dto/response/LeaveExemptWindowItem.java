package com.prafta.app.attd.attd01.dto.response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * OT 칩 정합(2026-08-08): 오늘/일상세 응답에 싣는 <b>연차 면제 구간</b> 표시용 아이템.
 *
 * <p><b>신설 배경</b> — 앱 초과근무 신청 폼({@code OvertimeForm.slotWindows})의 "등록 가능" 칩이
 * FE 자체 계산(실근태−스케줄)이라 연차 면제 구간을 반영하지 못했다. 반차일 재출근 구간에서
 * 칩은 전량을 보여주지만 서버 검증({@code ATTD_400_196})은 면제 겹침분을 거부해 칩과 신청 결과가
 * 불일치한다(웹 일자상세와 동일 계열 — 무인테스트 실측 2026-08-08). 산출은 서버 검증
 * ({@code AppReq07ServiceImpl.assertNoLeaveExemptOverlap})과 동일한 단일 진입점
 * ({@code PartialLeaveWindowUtils.exemptStampRange} + 그날 스케줄 프레임)을 재사용한다.
 *
 * <p>표현은 (일자, 시각) 쌍 — 앱 FE 의 stamp 축({@code stampOf(ymd, hhmm)})이 일자+시각 기반이라
 * 변환 없이 그대로 소비한다. 환산 불가 행은 검증(그날 OT 전면 거부)과 정합하도록 그날 전체
 * 구간으로 보수 변환된다(호출부 {@code fullDayBlockStampRange}).
 *
 * @param startDate 면제 시작 일자 (YYYYMMDD)
 * @param startTime 면제 시작 시각 (HHmm)
 * @param endDate   면제 종료 일자 (YYYYMMDD)
 * @param endTime   면제 종료 시각 (HHmm, exclusive)
 */
public record LeaveExemptWindowItem(
      String startDate
    , String startTime
    , String endDate
    , String endTime
) {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 공용 유틸 stamp 구간({@code int[]{start, end}}, 원점 = workYmd 전날 00:00)을 (일자, 시각)으로
     * 역변환한다. dayOffset = stamp/1440 − 1 (0=당일), 시각 = stamp%1440.
     */
    public static LeaveExemptWindowItem fromStampRange(String workYmd, int[] range) {
        LocalDate base = LocalDate.parse(workYmd, YMD);
        return new LeaveExemptWindowItem(
                base.plusDays(range[0] / 1440 - 1).format(YMD), toHhmm(range[0] % 1440),
                base.plusDays(range[1] / 1440 - 1).format(YMD), toHhmm(range[1] % 1440));
    }

    private static String toHhmm(int minOfDay) {
        return String.format("%02d%02d", minOfDay / 60, minOfDay % 60);
    }
}
