package com.prafta.common.cmm.leave.service;

/**
 * 연차 시간차 "1일 환산시간"(분모) 서비스
 * (LC-02 → 2026-07-21 480 고정 전환 → 개인 분모 개편 PC-03, D1).
 *
 * <p>2026-07-28 사용자 결정(D1): 분모를 <b>개인 기본 근무타입({@code tb_user.DEFAULT_SCH_CD})의
 * 소정근로분</b>으로 전환한다. 480 전사 고정 폐기.
 * <ul>
 *   <li>분모 해석 = "처리 시점의 DEFAULT_SCH_CD" + "대상일 기준 유효 스케줄 버전"
 *       (tb_sch_mgmt + _hist effective-dating — N4. 소급 재계산 없음).</li>
 *   <li>소정근로가 480분(8시간)을 초과하는 근무타입은 <b>480 캡</b>(§5-③ — 근로자 유리).</li>
 *   <li>산출 불가(DEFAULT_SCH_CD 미지정(교대 등)/스케줄 미존재/시각 비정상/0 이하)는
 *       {@code null} — fail-closed(N5). 시간차 차단 판정({@code calcHourlyCharge} 진입부,
 *       ATTD_400_193)과 표기 480 폴백은 호출부 책임 분리.</li>
 *   <li>tb_leave_conversion_policy 테이블은 계속 dormant(부활 금지 — 분모는 테이블이 아니라
 *       개인 근무타입에서 파생).</li>
 * </ul>
 *
 * <p>출처: 작업지시서_연차-개인분모-전환-및-짜투리-보전 D1·D2·N4·N5·N7 / plan PC-03
 * / 정책서 attd/08-leave.md §8.5.9
 */
public interface LeaveConversionPolicyService {

    /** 분모 폴백·캡 기준(분) — 재정산 폴백(§7-③)·표기 폴백·480 캡(§5-③)의 단일 상수. */
    int DEFAULT_CONV_MINUTES = 480;

    /**
     * 개인 분모(1일 환산시간, 분) 산출 — 시간차 차감(LC-03)·재정산(PC-01)·표기(N8)의 단일 출처.
     *
     * <p>ⓐ {@code tb_user.DEFAULT_SCH_CD} 조회(NULL/빈값 → null)
     * ⓑ 대상일 기준 유효 스케줄 버전(effective-dating, 폴백 최이른 버전 — N4)
     * ⓒ 소정근로분 산출({@code ScheduleWorkMinutesUtils} — 1·2구간 합산, 야간 보정, 휴게 차감)
     * ⓓ 480 캡 ⓔ 산출 불가 → {@code null}(fail-closed).
     *
     * @param workYmd 대상일(YYYYMMDD) — 스케줄 버전 해석 기준
     * @return 분모(분, 1~480). 산출 불가면 {@code null}.
     */
    Integer resolvePersonalConvMinutes(String cmpnyCd, String userCd, String workYmd);
}
