package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 특정 사용자/일자의 근무 스케줄 시각 운반체.
 *
 * <p>{@code tb_user_work_plan}(사용자 근무 계획) → {@code tb_sch_mgmt}(스케줄 마스터)를
 * 조인하여 해당 일자에 적용되는 1구간/2구간 시각·휴게(분)을 담는다.
 *
 * <p>시간차 연차 차감(prafta-019-A §2.4)에서 "그날 1일 소정근로분"을 계산하는 입력으로 쓰인다.
 * WORK_PLAN_CD 가 연차 코드(LEAVE_CD)이거나 스케줄이 없으면 본 VO는 조회되지 않는다(null).
 */
@Getter
@Setter
public class DailyScheduleVO {

    /** 적용 스케줄 코드 (tb_sch_mgmt.SCH_CD) */
    private String schCd;

    /** 1구간 시작시각 (HHMM) */
    private String fstSchStrTime;

    /** 1구간 종료시각 (HHMM) */
    private String fstSchEndTime;

    /** 1구간 휴게(분) */
    private String fstSchBrkMin;

    /** 2구간 시작시각 (HHMM, 없으면 null) */
    private String secSchStrTime;

    /** 2구간 종료시각 (HHMM, 없으면 null) */
    private String secSchEndTime;

    /** 2구간 휴게(분, 없으면 null) */
    private String secSchBrkMin;

    /** 1구간 휴게 시작시각 (HHMM, 없으면 null) — 휴게 가로지름 판정용 (prafta-019-E) */
    private String fstBrkStrTime;

    /** 1구간 휴게 종료시각 (HHMM, 없으면 null) */
    private String fstBrkEndTime;

    /** 2구간 휴게 시작시각 (HHMM, 없으면 null) */
    private String secBrkStrTime;

    /** 2구간 휴게 종료시각 (HHMM, 없으면 null) */
    private String secBrkEndTime;
}
