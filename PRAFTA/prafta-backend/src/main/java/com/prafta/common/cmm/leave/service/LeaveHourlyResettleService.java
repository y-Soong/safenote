package com.prafta.common.cmm.leave.service;

/**
 * 시간차 연차 취소·반려 재정산 서비스 (연차 시간차 환산 개편 LC-05 — F1 최중요 보완 + F2).
 *
 * <p>하한 차액(R3)은 "나중 신청 건"에 얹히는 구조라, 그날 임의 건의 취소·반려·삭제가
 * 잔존 건들의 차감액을 전부 무효화한다. 예) 100분(0.2083) + 110분(누적 하한으로 0.2917 부과)
 * 중 <b>첫 건 취소</b> → 잔존 110분 건은 단독 기준 0.2292 여야 하나 0.2917 로 잔류.
 * {@code recomputeGrantUsedDays} 는 use 행 합계를 그대로 믿으므로 use 행 자체를 고치는
 * 본 재정산 없이는 원장(USED_DAYS)이 과대/과소로 영구 잔류한다(plan §4).
 *
 * <p>훅 지점 3곳(plan §2 LC-05-①):
 * <ul>
 *   <li>{@code LeaveFlowServiceImpl.rejectStep} — cancelLeaveUseByReqId 직후(웹 + 앱 결재·앱
 *       관리자 승인 전부 이 경로로 위임 — plan §0-2).</li>
 *   <li>{@code LeaveFlowServiceImpl.approveStep} — 최종 승인 분기(F2 승인 시점 재판정 —
 *       결재 대기 중 같은 날 다른 건이 취소·반려로 빠졌을 때 확정값 보정).</li>
 *   <li>{@code Attd13ServiceImpl.applyDelete} — 연차 변경/삭제 동의 반영 후(시간차 행 삭제 시).</li>
 * </ul>
 *
 * <p>코어 산식은 {@code HourlyLeaveChargeUtils}(LC-03)를 그대로 공유한다 — 신청 시 계산과
 * 재정산이 항상 같은 값을 내도록 단일 출처화(지시서 F1).
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 T2·F1·F2 / plan §2 LC-05·§4
 * / 정책서 attd/08-leave.md §8.5.8(이력 보존 — soft cancel 유지)
 */
public interface LeaveHourlyResettleService {

    /**
     * 그날 잔존 시간차(02/03/04, CONFIRMED) 건을 시간순(START_TIME, LEAVE_ID) 재적용해
     * 각 건 LEAVE_DAYS 를 재산출하고, 영향 GRANT 전부 USED_DAYS 를 재집계한다.
     *
     * <p>동시성: 신청 흐름(LC-04, F5)과 동일한 advisory lock 키
     * ({@code HourlyLeaveChargeUtils.leaveDayLockKey})로 직렬화 — 재정산 중 동시 신청 차단.
     *
     * <p>잔존 시간차가 없으면 no-op(취소분의 GRANT 재계산은 호출부가 이미 수행).
     * 재정산은 하한 차액을 잔존 건으로 재배치할 뿐이므로 그날 합계는 항상 이전 이하 —
     * 잔여 초과가 새로 발생하지 않는다(plan LC-05-②).
     *
     * @param workYmd     대상 근무일(YYYYMMDD) — 취소/승인된 시간차 행의 START_DATE
     * @param actorUserCd 수행자(UPDATE_NO 기록)
     */
    void resettleHourlyLeaveOnDate(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                   String actorUserCd);
}
