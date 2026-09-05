package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 특정 사용자/일자의 근무 스케줄 <b>고정연장</b> 시각 운반체 (부분휴가 휴게 넘김 v2 BW2-02, 2026-09-05).
 *
 * <p>{@code tb_user_work_plan}(사용자 근무 계획) → {@code tb_sch_mgmt}(/{@code _hist} 유효 버전)를 조인해
 * 소정 1·2구간 시각 4개(고정연장 익일 배치 판정용)와 전방·후방 고정연장 HHMM 쌍 4개를 담는다.
 * {@code FixedOtScheduleUtils.totalFixedOtMinutes} 의 8인자 입력과 1:1 이다.
 *
 * <p>★PRAFTA-FIXEDOT-2 가드: {@link DailyScheduleVO} 에 고정연장 필드를 추가하지 못하므로(연차 분모 오염 방지)
 * 법정 휴게 하한 산식의 잔여 근로 {@code R = D − X + F}(요청서 §1-1·R5)에 쓰는 F 만 이 VO 로 <b>별도 조회</b>한다.
 * 연차 분모(D/E1) 산출에는 절대 쓰지 않는다.
 */
@Getter
@Setter
public class DailyFixedOtVO {

    /** 소정 1구간 시작시각 (HHMM) */
    private String fstSchStrTime;

    /** 소정 1구간 종료시각 (HHMM) */
    private String fstSchEndTime;

    /** 소정 2구간 시작시각 (HHMM, 없으면 null) */
    private String secSchStrTime;

    /** 소정 2구간 종료시각 (HHMM, 없으면 null) */
    private String secSchEndTime;

    /** 전방 고정연장 시작시각 (HHMM, 없으면 null) */
    private String preFixedOtStrTime;

    /** 전방 고정연장 종료시각 (HHMM, 없으면 null) */
    private String preFixedOtEndTime;

    /** 후방 고정연장 시작시각 (HHMM, 없으면 null) */
    private String fixedOtStrTime;

    /** 후방 고정연장 종료시각 (HHMM, 없으면 null) */
    private String fixedOtEndTime;
}
