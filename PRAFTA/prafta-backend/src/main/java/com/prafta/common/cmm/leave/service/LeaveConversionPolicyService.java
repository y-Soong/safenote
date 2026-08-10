package com.prafta.common.cmm.leave.service;

/**
 * 연차 시간차 "1일 환산시간"(분모) 서비스
 * (LC-02 → 2026-07-21 480 고정 전환 → 개인 분모 개편 PC-03 D1 → 당일분모 전환 E1).
 *
 * <p>2026-08-03 사용자 확정(E1): 실차감 분모를 <b>당일 배정 스케줄(TB_USER_WORK_PLAN → TB_SCH_MGMT)의
 * 소정근로분</b>으로 전환한다. 개인 기본 근무타입 분모(구 D1, {@link #resolvePersonalConvMinutes})는
 * <b>참고 표시 전용</b>으로 격하·존치한다.
 * <ul>
 *   <li><b>실차감(시간차 차감·재정산·이동 재차감·짜투리 발동 판정)</b> =
 *       {@link #resolveDailyConvMinutes} — 그날 D 계산({@code selectDailySchedule})과 단일 출처.</li>
 *   <li><b>E4 참고 분모</b> = {@link #resolvePersonalConvMinutes} — 기본 근무타입 근사치(미산출
 *       480 폴백은 호출부). <b>2026-08-09 표기 규약 변경</b>: FE 는 날짜 미정 문맥 잔여/부여를
 *       일 단위 단독 표기로 전환 — E4 는 내부 판정(짜투리 리포트 최소단위 등)·구버전 앱 호환
 *       (additive convMinutes 필드)용으로만 잔존한다(신 FE 표기 미사용).</li>
 *   <li>소정근로가 480분(8시간)을 초과하는 스케줄은 <b>480 캡</b>(E7 — 근로자 유리).</li>
 *   <li>산출 불가(미배정/스케줄 미존재/시각 비정상/0 이하)는 {@code null} — fail-closed.
 *       시간차 차단 판정({@code calcHourlyCharge} 진입부, ATTD_400_194)과 표기 폴백은 호출부 책임 분리.</li>
 *   <li>tb_leave_conversion_policy 테이블은 계속 dormant(부활 금지 — 분모는 테이블이 아니라
 *       스케줄에서 파생).</li>
 * </ul>
 *
 * <p>출처: 작업지시서_연차-시간차-당일분모-전환 E1·E4·E7 / plan T1
 * / 선행: 작업지시서_연차-개인분모-전환-및-짜투리-보전 D1·N4·N5 / 정책서 attd/08-leave.md §8.5.9
 */
public interface LeaveConversionPolicyService {

    /** 분모 폴백·캡 기준(분) — 재정산 폴백(레거시 근사)·표기 폴백·480 캡(E7)의 단일 상수. */
    int DEFAULT_CONV_MINUTES = 480;

    /**
     * <b>당일 분모</b>(1일 환산시간, 분) 산출 — 실차감(LC-03 시간차 차감·PC-01 재정산·attd13 이동
     * 재차감·PC-05 짜투리 발동 판정)의 단일 출처 (E1).
     *
     * <p>ⓐ 당일 배정 스케줄 조회({@code LeaveDeductionMapper.selectDailySchedule} —
     * TB_USER_WORK_PLAN 의 WORK_PLAN_CD → TB_SCH_MGMT(_HIST) effective-dating. 미배정/연차 코드면 null)
     * ⓑ 소정근로분 산출({@code ScheduleWorkMinutesUtils} — 1·2구간 합산, 야간 보정, 휴게 차감)
     * ⓒ 480 캡(E7) ⓓ 산출 불가 → {@code null}(fail-closed — E2 미배정일 시간차 차단의 근거).
     *
     * @param workYmd 신청 대상일(YYYYMMDD)
     * @return 분모(분, 1~480). 산출 불가면 {@code null}.
     */
    Integer resolveDailyConvMinutes(String cmpnyCd, String siteCd, String userCd, String workYmd);

    /**
     * 개인 분모(기본 근무타입 소정근로분, 분) 산출 — <b>E4 참고 표시 전용(실차감 사용 금지)</b>.
     *
     * <p>당일분모 전환(E1)으로 실차감에서 은퇴. 특정일이 없는 표기(잔여 "N일 H시간 M분" 환산,
     * 대시보드·소멸 임박 리포트·apply-meta convMinutes 등)에서만 사용한다 — 참고용 데이터이므로
     * 실제 스케줄과 편차가 생겨도 허용(사용자 확정 2026-08-03). 미산출 시 480 폴백은 호출부 책임.
     *
     * <p>ⓐ {@code tb_user.DEFAULT_SCH_CD} 조회(NULL/빈값 → null)
     * ⓑ 대상일 기준 유효 스케줄 버전(effective-dating, 폴백 최이른 버전 — N4)
     * ⓒ 소정근로분 산출({@code ScheduleWorkMinutesUtils}) ⓓ 480 캡 ⓔ 산출 불가 → {@code null}.
     *
     * @param workYmd 대상일(YYYYMMDD) — 스케줄 버전 해석 기준
     * @return 분모(분, 1~480). 산출 불가면 {@code null}.
     */
    Integer resolvePersonalConvMinutes(String cmpnyCd, String userCd, String workYmd);
}
