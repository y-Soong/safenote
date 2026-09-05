package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;

import com.prafta.common.cmm.leave.vo.RemnantCoverSummaryVO;
import com.prafta.common.cmm.leave.vo.RemnantReportVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO;

/**
 * 짜투리 잔여 보전 서비스 (PC-05 발동 D3~D6 / PC-06 회수 D7 / PC-07 집계·리포트 D9-②③).
 *
 * <p>발동 판정(D5, 단일 출처)·발동 처리(D6)·회수(D7)·관리자 집계를 담당한다. 웹/앱 신청 흐름이
 * 같은 빈을 호출한다(미러 중복 금지).
 *
 * <p>동시성(N9): 발동 판정~기록은 사용자 단위 advisory lock {@link #remnantLockKey} 으로
 * 직렬화한다. 신청 흐름(웹/앱)은 시간차 {@code leaveDay} lock 이후에 본 lock 을 획득한다
 * (획득 순서 leaveDay → leaveRemnant 고정 — 데드락 방지). 회수({@code reclaimIfPossible})는
 * 내부에서 자체 획득한다.
 *
 * <p>출처: 작업지시서_연차-개인분모-전환-및-짜투리-보전 D3~D7·D9·N9 / plan PC-05·06·07
 * / 정책서 attd/08-leave.md §8.5.5(대상 5종)·§8.5.7(권한)·§8.5.8(이력 보존)
 */
public interface LeaveRemnantCoverService {

    /** N9 직렬화 advisory lock 키(사용자 단위 — 판정이 합산 잔여·미래 예정이라 일 단위로는 부족). */
    static String remnantLockKey(String cmpnyCd, String userCd) {
        return "leaveRemnant:" + cmpnyCd + ":" + userCd;
    }

    /**
     * 발동 판정(D5 ⓐ~ⓔ) + 발동 계획 산출. 전 조건 충족 시에만 계획을 반환하고, 하나라도
     * 미충족이면 {@code null}(호출부는 기존 잔여 부족 거부 ATTD_400_051 유지).
     *
     * <ul>
     *   <li>ⓐ 활성 정책 ALLOW_REMNANT_ROUND_UP='Y'</li>
     *   <li>ⓑ 신청 leaveCd ∈ 대상 5종(SYS_BIRTHDAY·비법정 제외) + 신청 단위 = 그 사용자의
     *       최소 사용단위 "1건"(교대자(conv null)는 시간차 제외 후 최소 — 메인 세션 확정)</li>
     *   <li>ⓒ 대상 5종 합산 잔여 &lt; 신청 요금(chargeDays)</li>
     *   <li>ⓓ 합산 잔여 &gt; 0</li>
     *   <li>ⓔ 대상 5종 CONFIRMED 사용기록 중 실사용일 미도래(START_DATE ≥ 오늘) 0건</li>
     * </ul>
     *
     * <p>submit 경로는 본 메서드 호출 전 remnant lock 을 획득해야 한다(N9).
     * preview 경로는 lock 없이 호출(조회 전용 추정치 — 시간차 preview 관례 미러).
     *
     * @param leaveMinutes         시간차(02/03/04) 신청 분 — 고정단위는 null 허용
     * @param chargeDays           신청 요금(일) — 최소단위 정상 요금
     * @param personalConvMinutes  본인 분모(분) — 산출 불가(교대 등)면 null
     */
    RemnantTriggerPlanVO evaluateTrigger(String cmpnyCd, String userCd, String workYmd, String leaveCd,
                                         String useUnitType, Integer leaveMinutes, BigDecimal chargeDays,
                                         Integer personalConvMinutes);

    /**
     * 발동 처리(D6): 계획의 부여별 분할 차감(use 행 INSERT — 신청 REQ_ID·단위, LEAVE_MINUTES 첫 행만,
     * LEAVE_CD 는 부여 귀속) + TB_LEAVE_REMNANT_COVER INSERT(ACTIVE) + 영향 GRANT 재집계.
     * 호출자 트랜잭션에 참여한다(원장 음수 금지 — use 합 = 잔여 전액).
     *
     * <p>BW-04(Q-2 확정, 2026-09-04): {@code brkWaiveYn}(휴게 무시 요청 — 전 행 동일 값, 'Y' 면 REQ_DTIME=NOW())
     * 과 {@code evidenceFileId}(증빙 파일 — 첫 행에만, LEAVE_MINUTES 관례 동일)를 함께 전달한다.
     * 종전 시그니처에서 증빙 파일 ID 가 유실되던 결함을 같이 해소했다. 이동 재발동(Attd13)은 원 행 값을 승계한다.
     *
     * @param brkWaiveYn     휴게 무시 요청 'Y'/'N'(null 은 'N')
     * @param evidenceFileId 증빙 파일 ID(nullable)
     * @param brkWaiveMin    v2(BW2-04): 넘긴 휴게 분량(반차 W_eff / 시간차 편입분 / 기록 전용 0). 'Y' 일 때만 저장
     *                       (null 은 0 — 매퍼 IFNULL), 'N' 이면 NULL. 전 행 동일 값
     * @return 마지막 INSERT 한 사용기록 ID(무결재 즉시확정 PUSH 통보용)
     */
    String applyTrigger(String cmpnyCd, String siteCd, String userCd, String workYmd, String useUnitType,
                        String startTime, String endTime, Integer leaveMinutes, String reason, String reqId,
                        RemnantTriggerPlanVO plan, String actorUserCd, String brkWaiveYn, String evidenceFileId,
                        Integer brkWaiveMin);

    /**
     * D7 회수: 잔여 복원 시(반려/수정 승인/삭제 동의) 근무일 미도래(WORK_YMD &gt; 오늘) ACTIVE COVER 를
     * 복원 잔여 한도 내에서 정상 차감으로 전환한다(부분 회수 허용 — 메인 세션 확정).
     * 당일(WORK_YMD = 오늘)은 도래로 보고 유지한다. 호출자 트랜잭션에 참여한다.
     */
    void reclaimIfPossible(String cmpnyCd, String userCd, String actorUserCd);

    /**
     * T1(이동)·T2(삭제): 대상 REQ 의 ACTIVE cover 를 무효화(CANCELLED)한다 — 회수 use INSERT 없이
     * 상태만 전환. REQ 전 use 행 취소와 같은 트랜잭션에서, {@link #reclaimIfPossible} 호출 <b>전</b>에
     * 수행해야 한다(취소 직후 회수 훅이 그 REQ 로 새 use 행을 만들어 삭제한 휴가가 부활하는 것을 차단 —
     * plan §0-1-2). 호출자 트랜잭션에 참여한다.
     *
     * @return 무효화된 cover 행 수
     */
    int cancelCoversByReq(String cmpnyCd, String reqId, String actorUserCd);

    /** D9-②: 연간 회사 부담 집계(N일/M건) + 상세 목록. 관리자(MASTER/HR) 전용(진입부 강제 §8.5.7). */
    RemnantCoverSummaryVO getCoverSummary(String cmpnyCd, String authCd, String year);

    /**
     * D9-③·N2: 소멸 임박 짜투리 리포트 — 0 &lt; 잔여 &lt; 본인 최소단위 요금인 사용자 목록.
     * 잔여 &lt; 0.001 은 "절사 끝수"로 구분(§5-④ 확정 임계). 관리자(MASTER/HR) 전용.
     */
    RemnantReportVO getRemnantReport(String cmpnyCd, String authCd);
}
