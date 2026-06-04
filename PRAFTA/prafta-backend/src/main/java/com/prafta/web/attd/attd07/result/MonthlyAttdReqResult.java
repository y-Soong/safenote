package com.prafta.web.attd.attd07.result;

/**
 * Result row of {@code Attd07Mapper.selectMonthlyAttdReq}.
 *
 * PRAFTA-003: the underlying TB_USER_ATTD_REQ columns were renamed
 *   CHECK_IN_DATE  -> START_DATE
 *   CHECK_IN_TIME  -> START_TIME
 *   CHECK_OUT_DATE -> END_DATE
 *   CHECK_OUT_TIME -> END_TIME
 * Field names below were updated to match. Front-end mapping was migrated in the
 * same release; see AttdDayDetailPop.vue (line ~1310, ~1602).
 */
public record MonthlyAttdReqResult(
    /** company code */
      String cmpnyCd

    /** request id */
    , String reqId

    /** site code */
    , String siteCd

    /** node (department) code */
    , String nodeCd

    /** worker user code */
    , String userCd

    /** request type [SYS032] */
    , String reqType

    /** request type name (resolved via code master) */
    , String reqTypeNm

    /** request status [SYS033] */
    , String reqStatus

    /** request status name (resolved via code master) */
    , String reqStatusNm

    /** work sequence */
    , String workSeq

    /** work date (yyyyMMdd) */
    , String workYmd

    /** start date (yyyyMMdd) */
    , String startDate

    /** start time (HHmm) */
    , String startTime

    /** end date (yyyyMMdd) */
    , String endDate

    /** end time (HHmm) */
    , String endTime

    /** request reason */
    , String reqReason

    /** insert datetime */
    , String insertDate

    /** 연차(05/06) 결재 시 현재 로그인 사용자가 처리할 결재 단계 (그 외 타입/비결재자는 null) */
    , Integer approvalStep

    /**
     * 목표 스케줄 코드 (PRAFTA-APP-007).
     * 스케줄 수정 요청(REQ_TYPE='10')일 때 카드에 "변경 목표 스케줄"로 노출한다.
     * 그 외 요청 유형에서는 NULL.
     */
    , String schCd

    // ────────────────────────────────────────────────────────────────
    // PRAFTA-APP-007-WEB-5: 스케줄 수정(10) 카드 BEFORE/AFTER 표시용 원시 시각.
    //   목표(tgt*)는 REQ.SCH_CD → tb_sch_mgmt, 현재(cur*)는 tb_user_work_plan.WORK_PLAN_CD → tb_sch_mgmt.
    //   ⚠️ MyBatis record 는 컬럼을 "순서(위치)"로 생성자 인자에 바인딩한다. 아래 필드 순서는
    //      selectMonthlyAttdReq 의 SELECT 절 끝 컬럼 순서와 정확히 일치해야 한다(record 끝 = SELECT 끝).
    //   라벨은 프론트가 조립하므로 원시 HHmm 문자열만 내린다(BRK 미포함 — D12).
    //   type='10' 외 요청에서는 전부 NULL. WORK_PLAN_CD 가 LEAVE_CD(연차)면 cur* NULL.
    // ────────────────────────────────────────────────────────────────

    /** 목표 스케줄 타입 [SYS019] (라벨 미사용, 향후 확장용) */
    , String tgtSchType
    /** 목표 스케줄 1구간 시작 시각 (HHmm) */
    , String tgtFstStrTime
    /** 목표 스케줄 1구간 종료 시각 (HHmm) */
    , String tgtFstEndTime
    /** 목표 스케줄 2구간 시작 시각 (HHmm, 1구간이면 NULL) */
    , String tgtSecStrTime
    /** 목표 스케줄 2구간 종료 시각 (HHmm, 1구간이면 NULL) */
    , String tgtSecEndTime

    /** 현재 스케줄 타입 [SYS019] (라벨 미사용) */
    , String curSchType
    /** 현재 스케줄 1구간 시작 시각 (HHmm) */
    , String curFstStrTime
    /** 현재 스케줄 1구간 종료 시각 (HHmm) */
    , String curFstEndTime
    /** 현재 스케줄 2구간 시작 시각 (HHmm, 1구간이면 NULL) */
    , String curSecStrTime
    /** 현재 스케줄 2구간 종료 시각 (HHmm, 1구간이면 NULL) */
    , String curSecEndTime

    // ────────────────────────────────────────────────────────────────
    // PRAFTA-APP-018-D: 연차(05/06) 요청 카드의 사용단위·차감일수 표시용 컬럼.
    //   selectMyPendingLeaveApprovals(LeaveFlowMapper) 와 동일한 TB_USER_LEAVE_USE(U) 조인 산출.
    //   ⚠️ MyBatis record 는 컬럼을 "순서(위치)"로 생성자 인자에 바인딩한다. 아래 3필드는
    //      반드시 record 끝(= curSecEndTime 다음) 이고 SELECT 절 끝과 "동일 순서"여야 한다.
    //      중간 삽입 금지(전 필드 밀림 → 런타임 변환 폭발).
    //   01~04/10 요청행은 U 미매칭 → useUnitType/unitNm NULL, leaveDays 는 A.LEAVE_DAYS(연차 외 NULL).
    // ────────────────────────────────────────────────────────────────

    /** 차감 일수 (TB_USER_ATTD_REQ.LEAVE_DAYS, 문자열 그대로 — FE 가 정규화) */
    , String leaveDays
    /** 사용 단위 코드 [SYS025] (00종일/01반차/02 2시간/03 1시간/04 30분, U 미매칭 시 NULL) */
    , String useUnitType
    /** 사용 단위 한글 라벨 (SYS025 FNC 산출: 종일/반차/2시간/1시간/30분, U 미매칭 시 NULL) */
    , String unitNm
) {
}
