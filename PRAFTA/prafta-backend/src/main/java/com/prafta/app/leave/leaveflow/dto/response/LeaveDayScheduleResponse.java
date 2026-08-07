package com.prafta.app.leave.leaveflow.dto.response;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 앱 연차 신청 폼 일자별 스케줄(휴게 포함) 응답.
 *
 * <p>시간차 연차는 휴게시간을 가로지를 수 없으므로(ATTD_400_055), 신청 시각 선택 전에
 * 해당 일자의 근무/휴게 구간을 사용자에게 안내하기 위한 조회 전용 응답이다.
 * 시각은 모두 HHMM 문자열(미설정 구간은 null). 스케줄이 없는 날(근무계획 없음/연차 배정)은
 * {@code hasSchedule=false} 로 내리고 시각 필드는 모두 null.
 *
 * @param hasSchedule   해당 일자에 적용된 근무 스케줄 존재 여부
 * @param fstSchStrTime 1구간 근무 시작(HHMM)
 * @param fstSchEndTime 1구간 근무 종료(HHMM)
 * @param secSchStrTime 2구간 근무 시작(HHMM, 없으면 null)
 * @param secSchEndTime 2구간 근무 종료(HHMM, 없으면 null)
 * @param fstBrkStrTime 1구간 휴게 시작(HHMM, 휴게시각 미설정이면 null)
 * @param fstBrkEndTime 1구간 휴게 종료(HHMM, 휴게시각 미설정이면 null)
 * @param secBrkStrTime 2구간 휴게 시작(HHMM, 없으면 null)
 * @param secBrkEndTime 2구간 휴게 종료(HHMM, 없으면 null)
 * @param halfDayBoundaryTime 반차 경계 시각(HHMM). 근로를 절반으로 나누는 시각(휴게 제외 누적 기준).
 *                            스케줄 없음/산출 불가면 null — HB-03(additive, 구 앱 무영향)
 * @param halfStartPartRange  시작기준(늦게 출근) 반차가 쉬는 구간 "HHMM~HHMM". 산출 불가면 null
 * @param halfEndPartRange    종료기준(일찍 퇴근) 반차가 쉬는 구간 "HHMM~HHMM". 산출 불가면 null
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
) {
    /** 스케줄 없는 날 응답(시각 전부 null). */
    public static LeaveDayScheduleResponse empty() {
        return new LeaveDayScheduleResponse(false, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    /** 스케줄 VO → 응답 매핑(반차 경계 필드 포함 — HB-03). */
    public static LeaveDayScheduleResponse from(DailyScheduleVO sch,
                                                String halfDayBoundaryTime,
                                                String halfStartPartRange,
                                                String halfEndPartRange) {
        if (sch == null) {
            return empty();
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
        );
    }
}
