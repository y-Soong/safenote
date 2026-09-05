package com.prafta.app.leave.leaveflow.dto.response;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 앱 연차 신청 화면의 근무일 스케줄 조회 응답(day-schedule).
 *
 * <p>HB-03: 반차 경계 미리보기 3필드({@code halfDayBoundaryTime}, {@code halfStartPartRange}, {@code halfEndPartRange})는
 * <b>END·미체크 경계</b> 기준 — 의미 불변(구 앱 무영향, plan §4-1).
 *
 * <p>v2 법정 하한 상한제(BW2-04, 2026-09-05): v1 "전부 넘김" 전용 필드(체크 시 파트별 경계·구간·recordOnly)와
 * 법정 경고 묶음은 제거했다(파트·W 기준 결과 시각은 preview 로 이동 — §7 Q3). day-schedule 은 cap·step·movable 만 내린다.
 * <ul>
 *   <li>{@code halfDayBoundaryTimeStart} — START(늦게 출근) 미체크 경계(G-3 끝걷기).</li>
 *   <li>{@code brkWaiveAllowYn} — 회사 토글(N 이면 섹션 미노출). {@code brkTimeRegisteredYn} — 휴게 시각 등록 여부(G-2 안내).</li>
 *   <li>{@code brkWaiveCapMin} — 법정 cap = max(0, B − req), X = H(=D/2). 스케줄 없음/산출 불가 → null.</li>
 *   <li>{@code brkWaiveStepMin} — 스테퍼 단위(15). {@code brkWaiveReasonText} — 서버 생성 안내(FE 재계산 금지).</li>
 *   <li>{@code halfWaiveMovableStartMin}/{@code halfWaiveMovableEndMin} — 파트별 근로 쪽 휴게분(R3 판정 근거).
 *       실효 cap = {@code min(brkWaiveCapMin, 해당 파트 값)} — FE 는 이 조합만 사용한다.</li>
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
    , String brkWaiveAllowYn
    , String brkTimeRegisteredYn
    , Integer brkWaiveCapMin
    , Integer brkWaiveStepMin
    , String brkWaiveReasonText
    , Integer halfWaiveMovableStartMin
    , Integer halfWaiveMovableEndMin
) {

    /** 스케줄 없음 응답(경계·cap 전부 null, 회사 토글만 전달). */
    public static LeaveDayScheduleResponse empty(String brkWaiveAllowYn) {
        return new LeaveDayScheduleResponse(false, null, null, null, null, null, null, null, null,
                null, null, null, null,
                brkWaiveAllowYn, "N",
                null, null, null, null, null);
    }

    /**
     * 스케줄 있음 응답.
     *
     * @param sch                      그날 스케줄(null 이면 {@link #empty(String)})
     * @param halfDayBoundaryTime      END·미체크 경계(HB-03 불변)
     * @param halfStartPartRange       HB-03 불변(END 경계 공용값)
     * @param halfEndPartRange         HB-03 불변
     * @param halfDayBoundaryTimeStart START·미체크 경계(G-3)
     * @param brkWaiveCapMin           법정 cap(산출 불가 null)
     * @param brkWaiveStepMin          스테퍼 단위(15)
     * @param brkWaiveReasonText       서버 안내 문구(산출 불가 null)
     * @param halfWaiveMovableStartMin START 파트 근로 쪽 휴게분
     * @param halfWaiveMovableEndMin   END 파트 근로 쪽 휴게분
     */
    public static LeaveDayScheduleResponse of(DailyScheduleVO sch,
                                              String halfDayBoundaryTime,
                                              String halfStartPartRange,
                                              String halfEndPartRange,
                                              String halfDayBoundaryTimeStart,
                                              String brkWaiveAllowYn,
                                              String brkTimeRegisteredYn,
                                              Integer brkWaiveCapMin,
                                              Integer brkWaiveStepMin,
                                              String brkWaiveReasonText,
                                              Integer halfWaiveMovableStartMin,
                                              Integer halfWaiveMovableEndMin) {
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
            , brkWaiveAllowYn
            , brkTimeRegisteredYn
            , brkWaiveCapMin
            , brkWaiveStepMin
            , brkWaiveReasonText
            , halfWaiveMovableStartMin
            , halfWaiveMovableEndMin
        );
    }
}
