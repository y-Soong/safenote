package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;

import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.BreakMergeResult;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfPart;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;

/**
 * 시간차 연차 동적 차감 계산 서비스 (prafta-019-A §2.4 → 연차 시간차 환산 개편 LC-03).
 *
 * <p>연차 시간차 환산 개편(LC-03)으로 분모가 "1일 환산시간"({@link LeaveConversionPolicyService})으로
 * 추상화되었고(R1), 개인 분모 개편(PC-03 D1)을 거쳐 당일분모 전환(E1, 2026-08-03 확정)으로
 * 그 환산시간이 <b>당일 배정 스케줄 소정근로분</b>(480 캡 E7, 산출 불가 시 시간차 차단 ATTD_400_194)이
 * 되었다. 그날 시간차 누적 분 기준의 하한 가드(R3, 3단 마일스톤)와 상한 캡(R4, 1.0일)을 적용해
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
     * 반차 경계 산출 (반차 시간대 도입 HB-01/02, 2026-08-07).
     *
     * <p>그날 배정 스케줄을 조회해 {@code ScheduleWorkMinutesUtils.halfDayBoundary} 로 위임한다.
     * 경계 = 근무 시작부터 근로만 누적해 {@code D/2}(정수 절사)에 도달하는 시각이며,
     * 반환 {@code exemptMinutes} 는 차감 {@code LEAVE_MINUTES}(= daily/2)와 <b>같은 값</b>이어야 한다.
     *
     * @return 경계 산출 결과. 근무 계획/스케줄이 없거나 계산 불가 시 {@code null}(호출부가 ATTD_400_110 거부).
     */
    HalfDayBoundary getHalfDayBoundary(String cmpnyCd, String siteCd, String userCd, String workYmd);

    /**
     * 반차 경계 산출 — 파트·휴게 무시 체크 여부 지정 (부분휴가 휴게 무시 도입 BW-02, 2026-09-04).
     *
     * <p>그날 배정 스케줄을 조회해 {@code ScheduleWorkMinutesUtils.halfDayBoundary(sch, part, waive)} 로 위임한다.
     * 4-인자 시그니처는 {@code (END, false)} 와 결과가 같다(무회귀). 늦게 출근(START)은 종료에서 거꾸로 걷는
     * 파트별 경계(G-3)이며, 체크(waive) 요청은 결과 {@code recordOnly} 로 시각 불변·기록 전용 여부를 알린다.
     *
     * @param part  반차 파트(START/END). {@code null} 이면 END.
     * @param waive 휴게 무시 체크 여부
     * @return 경계 산출 결과. 근무 계획/스케줄이 없거나 계산 불가 시 {@code null}(호출부가 ATTD_400_110 거부).
     */
    HalfDayBoundary getHalfDayBoundary(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                       HalfPart part, boolean waive);

    /**
     * 시간차 휴게 무시 체크 시 "붙은 휴게 편입" 산출 (부분휴가 휴게 무시 도입 BW-03, 2026-09-04 — 요청서 §1-2).
     *
     * <p>그날 배정 스케줄을 조회해 {@code ScheduleWorkMinutesUtils.mergeAdjacentBreaks} 로 위임한다.
     * 신청 {@code [startMin, endMin)} 에 접하거나 겹치는 휴게 시각 구간을 쉬는 구간에 합친 저장 시각과,
     * 차감 분(= 신청 길이 − 휴게 겹침)을 돌려준다. 체크 경로 전용 — 미체크는 종전 {@link #crossesBreak} 거부.
     *
     * @return 편입 결과. 스케줄이 없거나 산출 불가 시 {@code null}(호출부가 ATTD_400_110 거부).
     */
    BreakMergeResult mergeAdjacentBreaks(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                         int startMin, int endMin);

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
     *   <li>분모 = 당일 배정 스케줄 소정근로분(E1 — {@code selectDailySchedule} 조회를 D 산출과
     *       공유, 480 캡 E7. {@code LeaveConversionPolicyService.resolveDailyConvMinutes} 와 동일 산식).
     *       산출 불가(미배정 등)면 ATTD_400_194 로 시간차 차단(E2 fail-closed, 단일 출처).</li>
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

    /**
     * sec N-2(2026-08-07): 신청/이동하려는 연차 시간대가 그날 이미 확정된 "시각 보유" 연차
     * (반차 '01' + 시간차 '02'~'04')와 겹치는지 판정한다(겹치면 호출부가 {@code ATTD_400_112} 거부).
     *
     * <p>종전에는 매퍼 SQL(웹 {@code LeaveFlowMapper} / 앱 {@code AppLeaveFlowMapper} 의
     * {@code countOverlappingTimeLeaveOnDate})이 CONCAT(DATE,TIME) 12자리 비교에 wrap CASE 를 붙여
     * 판정했는데, <b>각 행이 자기 {@code END_TIME < START_TIME} 일 때만</b> +1일 보정하는 구조라
     * 한쪽만 wrap 되는 조합(야간 시작기준 반차 {@code 2200~0200} vs 시간차 {@code 0030~0130})에서
     * 프레임이 어긋나 겹침을 놓쳤다. 판정을 Java 로 옮기고 절대 시각 환산은
     * {@code PartialLeaveWindowUtils.exemptStampRange}(그날 원 스케줄 프레임) 단일 진입점에 위임한다.
     *
     * <p>그날 스케줄을 얻지 못하거나 시각 환산이 불가하면 <b>겹침으로 본다</b>(fail-closed, §15-2-3).
     * 반차·시간차는 모두 선행 가드에서 스케줄 존재를 이미 확인하므로 정상 경로에서는 도달하지 않는다.
     *
     * @param startHhmm 신청/이동 대상 시각 시작(HHmm)
     * @param endHhmm   신청/이동 대상 시각 종료(HHmm)
     * @return 겹치면 {@code true}. 시각이 없으면(고정단위 종일 등) {@code false}.
     */
    boolean overlapsTimeLeaveOnDate(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                    String startHhmm, String endHhmm);
}
