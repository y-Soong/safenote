package com.prafta.common.cmm.leave.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.prafta.common.cmm.leave.service.LeaveDeductionService;

/**
 * 시간차 연차 환산 코어 산식 (연차 시간차 환산 개편 LC-03 — 순수 함수, 단일 출처).
 *
 * <p>신청 흐름(LC-04 {@code calcHourlyCharge})과 취소·반려 재정산(LC-05
 * {@code LeaveHourlyResettleService})이 <b>반드시 본 클래스를 공유</b>한다 — 신청 시 계산과
 * 재정산이 항상 같은 값을 내도록 산식을 단일 출처화한다(지시서 F1).
 *
 * <p>확정 규칙(지시서 §2 / 설계 문서 §0 / 개인 분모 개편 PC-03 §5-② N2):
 * <ul>
 *   <li>R1 환산: raw = 그날 시간차 누적 분 ÷ "1일 환산시간"(conv — PC-03 부터 개인 기본
 *       근무타입 소정근로분, 480 캡. {@code LeaveConversionPolicyService} 단일 출처).</li>
 *   <li>R2 DOWN 절사(개정 — N2): 개인 분모는 420/450 등 <b>순환소수 분모</b>가 정상 유입되므로
 *       유한소수 강제({@link #isTerminatingConvMinutes(int)})는 역할 종료. 나눗셈의
 *       {@code RoundingMode.DOWN}(소수 5자리, decimal(8,5))이 <b>정식 규칙</b>이다 —
 *       절사 끝수는 근로자 유리 방향(과소 차감)으로만 발생한다.</li>
 *   <li>R3 하한 가드: 누적 분이 고정단위 시간(반반차 D/4·반차 D/2·종일 D)에 도달하면
 *       그날 차감 합계에 해당 단위 요금(0.25/0.5/1.0)을 하한으로 적용.
 *       마일스톤은 반반차 신설 확정(결정 ①)으로 처음부터 3단이다.</li>
 *   <li>R4 상한 캡: 하루 시간차 누적 차감 ≤ 1.0일 (D&gt;480 스케줄 보호).</li>
 * </ul>
 *
 * <p><b>M5(당일분모 전환 E1, 2026-08-03)</b>: conv 가 당일 배정 스케줄 소정근로분으로 전환되어
 * conv == D 가 구조적으로 성립한다(차이는 E7 480 캡뿐 — D&gt;480 스케줄에서만 conv&lt;D).
 * 따라서 R3 하한은 raw 와 자동 일치(D≤480 스케줄에서 실질 무발동)하고, R4 캡은 D&gt;480 케이스의
 * 보호 장치로만 동작한다. <b>1차 배포에서는 양쪽 모두 유지</b>(회귀 안전) — 무해화 확인 후 2차 제거는
 * 별도 작업(지시서 M5 권장안).</p>
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 §2(R1~R4) / 설계 문서 §1(수치표)·§2(쪼개기 누적 판정)
 * / plan §2 LC-03-② / 정책서 attd/08-leave.md §8.5.9
 */
public final class HourlyLeaveChargeUtils {

    /** 차감 일수 소수 자릿수(tb_user_leave_use.LEAVE_DAYS = decimal(8,5)) — 기존 상수 단일 출처 재사용. */
    private static final int SCALE = LeaveDeductionService.DEDUCTION_SCALE;

    /** 종일 요금(1.0일). */
    private static final BigDecimal FULL_DAY = new BigDecimal("1.00000");
    /** 반차 요금(0.5일). */
    private static final BigDecimal HALF_DAY = new BigDecimal("0.50000");
    /** 반반차 요금(0.25일) — 결정 ① 반반차 신설로 3단 마일스톤. */
    private static final BigDecimal QUARTER_DAY = new BigDecimal("0.25000");
    /** 0일(scale 통일). */
    private static final BigDecimal ZERO_DAYS = new BigDecimal("0.00000");

    /** 시간차 최소 신청 단위(분) — SYS025 '04'(30분). conv 유한소수 검증의 분자 기준. */
    private static final long MIN_UNIT_MINUTES = 30L;
    /** 소수 5자리 유한소수 판정용 10^5. */
    private static final long SCALE_POW10 = 100_000L;

    private HourlyLeaveChargeUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 순수 환산(R1·R2): {@code 누적 분 ÷ 환산시간(conv)}.
     *
     * <p>R2 개정(PC-03, N2): {@code RoundingMode.DOWN}(소수 5자리) 절사가 <b>정식 규칙</b>이다.
     * 개인 분모(420/450 등 순환소수 분모)에서 발생하는 절사 끝수(≤0.00012 수준)는 근로자 유리
     * 방향(과소 차감)이며, 짜투리 보전(ON)/소멸 리포트(OFF)가 후속 처리한다.
     *
     * @return 환산 일수(scale=5). 입력이 유효하지 않으면 0.
     */
    public static BigDecimal rawDays(int cumMinutes, int convMinutes) {
        if (cumMinutes <= 0 || convMinutes <= 0) {
            return ZERO_DAYS;
        }
        return BigDecimal.valueOf(cumMinutes)
                .divide(BigDecimal.valueOf(convMinutes), SCALE, RoundingMode.DOWN);
    }

    /**
     * 하한 가드(R3): 그날 시간차 누적 분이 도달한 최대 마일스톤의 고정단위 요금.
     *
     * <p>마일스톤(3단): 누적 ≥ D → 1.0 / 누적 ≥ D/2 → 0.5 / 누적 ≥ D/4 → 0.25 / 미달 → 0.
     * D(그날 소정근로분)가 2·4의 배수가 아니어도 정수 곱 비교({@code cum*2 >= D})라 경계가 exact 하다.
     *
     * @param dailyStdMinutes 그날 소정근로분(D). com-016-D filterLockedDays 잠금으로 사후 불변.
     *                        {@code null}/비양수면 하한 미적용(0) — plan LC-05-② 의사코드.
     */
    public static BigDecimal milestoneFloorDays(int cumMinutes, Integer dailyStdMinutes) {
        if (dailyStdMinutes == null || dailyStdMinutes <= 0 || cumMinutes <= 0) {
            return ZERO_DAYS;
        }
        int d = dailyStdMinutes;
        if (cumMinutes >= d) {
            return FULL_DAY;
        }
        if ((long) cumMinutes * 2 >= d) {
            return HALF_DAY;
        }
        if ((long) cumMinutes * 4 >= d) {
            return QUARTER_DAY;
        }
        return ZERO_DAYS;
    }

    /**
     * 그날 시간차 차감 합계(R1~R4 종합): {@code min( max(raw, floor), 1.0 )}.
     *
     * <p>개별 건 부과액은 {@code 본 합계 − 직전 누적 차감 합}의 차액이다(설계 문서 §2 누적 판정).
     */
    public static BigDecimal dayTotalDays(int cumMinutes, int convMinutes, Integer dailyStdMinutes) {
        BigDecimal raw = rawDays(cumMinutes, convMinutes);
        BigDecimal floor = milestoneFloorDays(cumMinutes, dailyStdMinutes);
        return raw.max(floor).min(FULL_DAY);
    }

    /**
     * (구 R2) 30분 단위 신청분이 소수 5자리(LEAVE_DAYS decimal(8,5)) 안에서
     * 정확히 나누어떨어지는 환산시간인지 판정한다.
     *
     * <p>모든 시간차 신청분은 30분의 배수이므로 {@code 30/conv} 가 5자리 유한소수이면
     * (⇔ {@code 30×10^5 % conv == 0}) 임의 누적 분의 환산값도 유한소수다.
     * 예: 480 ✓(0.0625) / 600 ✓(0.05) / 750 ✓(0.04) / 420 ✗(1/14 순환) / 360 ✗(1/12 순환).
     *
     * @deprecated 개인 분모 개편(PC-03, N2)으로 역할 종료 — 개인 소정근로분은 유한소수를 강제할 수
     *             없어(420/450 등 정상 유입) R2 는 "DOWN 절사 정식 규칙"으로 개정되었다. 호출처 0건,
     *             회귀 대비 한시 유지 후 제거 예정.
     */
    @Deprecated
    public static boolean isTerminatingConvMinutes(int convMinutes) {
        return convMinutes > 0 && (MIN_UNIT_MINUTES * SCALE_POW10) % convMinutes == 0;
    }

    /**
     * F5 동시성 직렬화 advisory lock 키(사용자·일 단위) — 시간차 신청(LC-04)과
     * 취소·반려 재정산(LC-05)이 <b>같은 키</b>로 상호 배타된다. 기존 leave01 lock 패턴 미러.
     */
    public static String leaveDayLockKey(String cmpnyCd, String userCd, String workYmd) {
        return "leaveDay:" + cmpnyCd + ":" + userCd + ":" + workYmd;
    }
}
