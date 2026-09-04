package com.prafta.app.leave.leaveflow.dto.response;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 앱 연차 신청 화면의 근무일 스케줄 조회 응답(day-schedule).
 *
 * <p>HB-03: 반차 경계 미리보기 3필드({@code halfDayBoundaryTime}, {@code halfStartPartRange}, {@code halfEndPartRange})는
 * <b>END·미체크 경계</b> 기준 — 의미 불변(구 앱 무영향, plan §4-1).
 *
 * <p>BW-06(2026-09-04, 부분휴가 휴게 무시): 체크/미체크 × START/END 4조합의 경계 구간과 법정 휴게 하한 경고를 모두
 * 내려 화면이 서버값만 표시한다(클라 재계산 금지). 모두 additive.
 * <ul>
 *   <li>{@code halfDayBoundaryTimeStart} — START(늦게 출근) 미체크 경계(G-3 끝걷기). 기존 {@code halfStartPartRange} 는
 *       END 경계를 공용한 값이라 G-3 이후엔 이 값과 다를 수 있다.</li>
 *   <li>{@code halfStartWaiveRange}/{@code halfEndWaiveRange} — 체크 시 쉬는 구간("HHMM~HHMM"). recordOnly 면 미체크 값과 같다.</li>
 *   <li>{@code halfDayBoundaryTimeWaiveStart}/{@code halfDayBoundaryTimeWaiveEnd} — 체크 시 파트별 경계(HHMM).</li>
 *   <li>{@code brkWaiveAllowYn} — 회사 토글(N 이면 체크박스 미노출). {@code brkTimeRegisteredYn} — 휴게 시각 등록 여부(G-2 안내).</li>
 *   <li>{@code brkWaiveRecordOnlyYn} — 양 파트 모두 체크해도 시각이 바뀌지 않으면 'Y'(휴게 시각 미등록·휴게가 쉬는 구간 안).
 *       파트별 값은 {@code halfLegalWarn.start/end.waive.recordOnlyYn}.</li>
 *   <li>{@code halfLegalWarn} — {@code { start: { plain, waive }, end: { plain, waive } }}, 각 {@code {warnYn, msg, recordOnlyYn}}.</li>
 * </ul>
 */
public record LeaveDayScheduleResponse(
      boolean hasSchedule
    , String fstSchStrTime
    , String fstSchEndTime
    , String secSchStrTime
    , String secSchEndTime
    , String fstBrkStrTime
    , String fstBrkEndTime
    , String secBrkStrTime
    , String secBrkEndTime
    , String halfDayBoundaryTime
    , String halfStartPartRange
    , String halfEndPartRange
    , String halfDayBoundaryTimeStart
    , String halfDayBoundaryTimeWaiveStart
    , String halfDayBoundaryTimeWaiveEnd
    , String halfStartWaiveRange
    , String halfEndWaiveRange
    , String brkWaiveAllowYn
    , String brkTimeRegisteredYn
    , String brkWaiveRecordOnlyYn
    , HalfLegalWarn halfLegalWarn
) {

    /** 파트별(START/END) 법정 경고 묶음. */
    public record HalfLegalWarn(PartWarn start, PartWarn end) {
    }

    /** 미체크(plain) / 체크(waive) 법정 경고. */
    public record PartWarn(WarnInfo plain, WarnInfo waive) {
    }

    /** 경고 1건: warnYn 'Y'/'N', msg(서버 문구, 없으면 null), recordOnlyYn(체크 요청이 기록 전용인지 — plain 은 항상 'N'). */
    public record WarnInfo(String warnYn, String msg, String recordOnlyYn) {
    }

    /** 스케줄 없음 응답(경계·경고 전부 null, 회사 토글만 전달). */
    public static LeaveDayScheduleResponse empty(String brkWaiveAllowYn) {
        return new LeaveDayScheduleResponse(false, null, null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null,
                brkWaiveAllowYn, "N", null, null);
    }

    /**
     * 스케줄 있음 응답.
     *
     * @param sch                    그날 스케줄(null 이면 {@link #empty(String)})
     * @param halfDayBoundaryTime    END·미체크 경계(HB-03 불변)
     * @param halfStartPartRange     HB-03 불변(END 경계 공용값)
     * @param halfEndPartRange       HB-03 불변
     */
    public static LeaveDayScheduleResponse of(DailyScheduleVO sch,
                                              String halfDayBoundaryTime,
                                              String halfStartPartRange,
                                              String halfEndPartRange,
                                              String halfDayBoundaryTimeStart,
                                              String halfDayBoundaryTimeWaiveStart,
                                              String halfDayBoundaryTimeWaiveEnd,
                                              String halfStartWaiveRange,
                                              String halfEndWaiveRange,
                                              String brkWaiveAllowYn,
                                              String brkTimeRegisteredYn,
                                              String brkWaiveRecordOnlyYn,
                                              HalfLegalWarn halfLegalWarn) {
        if (sch == null) {
            return empty(brkWaiveAllowYn);
        }
        return new LeaveDayScheduleResponse(
              true
            , sch.getFstSchStrTime()
            , sch.getFstSchEndTime()
            , sch.getSecSchStrTime()
            , sch.getSecSchEndTime()
            , sch.getFstBrkStrTime()
            , sch.getFstBrkEndTime()
            , sch.getSecBrkStrTime()
            , sch.getSecBrkEndTime()
            , halfDayBoundaryTime
            , halfStartPartRange
            , halfEndPartRange
            , halfDayBoundaryTimeStart
            , halfDayBoundaryTimeWaiveStart
            , halfDayBoundaryTimeWaiveEnd
            , halfStartWaiveRange
            , halfEndWaiveRange
            , brkWaiveAllowYn
            , brkTimeRegisteredYn
            , brkWaiveRecordOnlyYn
            , halfLegalWarn
        );
    }
}
