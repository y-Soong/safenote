package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;

/**
 * 시간차 연차 동적 차감 계산 서비스 (prafta-019-A §2.4).
 *
 * <p>prafta-017의 고정분수(1/0.5/0.25/0.125) 전제를 폐기하고, "그날 스케줄 기준
 * 1일 소정근로분"에 대한 신청분 비율로 차감 일수를 동적 환산한다.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1, §8.5.9
 *
 * <p>본 서비스는 작업 A에서 제공되며, 신청/검증/차감 흐름(작업 E)에서 소비된다.
 */
public interface LeaveDeductionService {

    /** 차감 일수 소수 자릿수 (tb_user_leave_use.LEAVE_DAYS = decimal(8,5)). */
    int DEDUCTION_SCALE = 5;

    /**
     * 사용자/일자의 1일 소정근로분(분)을 계산한다.
     *
     * <p>{@code tb_user_work_plan} → {@code tb_sch_mgmt} 조회 후
     * {@code (1구간 근로분) + (2구간 근로분)}을 합산한다.
     * 각 구간 근로분 = {@code (종료 - 시작) - 휴게(분)} (야간 구간은 +1440 보정).
     *
     * @return 1일 소정근로분(분, 양수). 근무 계획/스케줄이 없거나 계산 불가 시 {@code null}.
     */
    Integer getDailyStdWorkMinutes(String cmpnyCd, String siteCd, String userCd, String workYmd);

    /**
     * 시간차 차감 일수 = {@code 신청분 ÷ 1일 소정근로분} (decimal(8,5), 반올림 HALF_UP).
     *
     * <p>반차(0.5 고정)·1일(1.0)은 본 메서드 대상이 아니다(호출 측에서 별도 처리 — 작업 E).
     *
     * @param requestMinutes      신청 분 (양수)
     * @param dailyStdWorkMinutes 1일 소정근로분 (양수)
     * @return 차감 일수 (scale=5). 입력이 유효하지 않으면 {@code null}.
     */
    BigDecimal calcDeductionDays(int requestMinutes, int dailyStdWorkMinutes);

    /**
     * 신청 시간대 {@code [startMin, endMin)}가 그날 스케줄의 휴게 구간을 가로지르는지 판정한다
     * (정책서 §8.5.9 휴게 가로지름 거부 — prafta-019-E).
     *
     * <p>휴게 시작/종료 시각(tb_sch_mgmt.*_BRK_*_TIME)이 없거나 0폭이면 가로지름 없음으로 본다.
     *
     * @return 휴게 구간과 겹치면 {@code true}.
     */
    boolean crossesBreak(String cmpnyCd, String siteCd, String userCd, String workYmd,
                         int startMin, int endMin);
}
