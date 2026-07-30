package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;

import com.prafta.common.cmm.leave.vo.HourlyChargeVO;

/**
 * 시간차 연차 동적 차감 계산 서비스 (prafta-019-A §2.4 → 연차 시간차 환산 개편 LC-03).
 *
 * <p>연차 시간차 환산 개편(LC-03)으로 분모가 "그날 소정근로분"에서 "1일 환산시간"
 * ({@link LeaveConversionPolicyService})으로 전환되었고(R1), 개인 분모 개편(PC-03, D1)으로
 * 그 환산시간이 개인 기본 근무타입 소정근로분(480 캡, 산출 불가 시 시간차 차단)이 되었다.
 * 그날 시간차 누적 분 기준의 하한 가드(R3, 3단 마일스톤)와 상한 캡(R4, 1.0일)을 적용해
 * 이번 건 부과 차액을 산출한다 — {@link #calcHourlyCharge}.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1, §8.5.9
 * / 작업지시서_연차-시간차-환산-개편 §2(R1~R4)·F3 / 설계 문서 §1·§2
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
     * (구) 시간차 차감 일수 = {@code 신청분 ÷ 1일 소정근로분} (decimal(8,5), 반올림 HALF_UP).
     *
     * @param requestMinutes      신청 분 (양수)
     * @param dailyStdWorkMinutes 1일 소정근로분 (양수)
     * @return 차감 일수 (scale=5). 입력이 유효하지 않으면 {@code null}.
     * @deprecated 연차 시간차 환산 개편(LC-03)으로 {@link #calcHourlyCharge}가 대체.
     *             그날 소정근로 분모는 순환소수(7h=0.07143)·반올림 오차(30분×14회=1.00002)를
     *             유발한다. 호출처 0건(웹/앱 LC-04 전환 완료) — 회귀 대비 한시 유지 후 제거 예정.
     */
    @Deprecated
    BigDecimal calcDeductionDays(int requestMinutes, int dailyStdWorkMinutes);

    /**
     * 시간차 연차 이번 건 부과 차액 산출 (LC-03 — R1~R4 + F3 그날 누적 판정).
     *
     * <p>내부 처리:
     * <ol>
     *   <li>분모 = {@code LeaveConversionPolicyService.resolvePersonalConvMinutes(cmpnyCd, userCd, workYmd)}
     *       (개인 기본 근무타입 소정근로분, 대상일 기준 유효 버전, 480 캡 — PC-03 D1).
     *       산출 불가(교대 등)면 ATTD_400_193 으로 시간차 차단(D2·N5 fail-closed, 단일 출처).</li>
     *   <li>그날 기존 시간차(02/03/04) CONFIRMED 누적 분·누적 차감 합 조회 — <b>전 연차타입 합산</b>
     *       (F3, 타입을 나눠 쪼개는 우회 차단). 고정단위(종일/반차/반반차)는 누적에서 제외(plan §8-⑤).</li>
     *   <li>코어 산식({@code HourlyLeaveChargeUtils} — LC-05 재정산과 단일 출처):
     *       {@code dayTotal = min( max(누적분÷conv, 마일스톤 하한), 1.0 )},
     *       {@code charge = dayTotal − 기존 누적 차감 합} (차액 부과, 설계 문서 §2).</li>
     * </ol>
     *
     * <p>⚠ 동시성(F5): 같은 사용자·같은 날 누적 판정 레이스가 있으므로 호출부가
     * {@code HourlyLeaveChargeUtils.leaveDayLockKey} advisory lock 을 선획득해야 한다.
     *
     * @param requestMinutes 이번 신청 분 (양수)
     * @return 산출 결과(차액·판정 부가정보). 입력이 유효하지 않으면 {@code null}.
     */
    HourlyChargeVO calcHourlyCharge(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                    int requestMinutes);

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

    /**
     * 신청 시간대 {@code [startMin, endMin]}가 그날 스케줄의 정규 근무구간(1구간 또는 2구간) 안에
     * 완전히 포함되는지 판정한다. 시간차 연차는 "근무하는 시간"에서만 신청 가능하므로, 근무구간 밖
     * (예: 스케줄 07:00~15:00 인데 03:00~04:30 신청)은 거부 대상이다.
     *
     * <p>1구간 또는 2구간 중 한 구간에 {@code [start,end]}가 완전히 포함되면 {@code true}.
     * 스케줄이 없으면(근무시간 자체가 없음) {@code false}. 야간(종료&le;시작) 구간은 종료를 +1440 보정한다.
     *
     * @return 어느 근무구간에도 완전히 포함되지 않으면 {@code false}.
     */
    boolean withinScheduledWorkHours(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                     int startMin, int endMin);
}
