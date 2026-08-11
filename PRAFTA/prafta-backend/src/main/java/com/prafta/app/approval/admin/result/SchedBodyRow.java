package com.prafta.app.approval.admin.result;

/**
 * PRAFTA-APP-029-2(D6): 스케줄 수정(REQ_TYPE='10') 상세 본문 1행 — 현재 → 요청 스케줄 비교.
 *
 * <p>현재 스케줄(cur*) = 해당 사용자-일자 근무계획(TB_USER_WORK_PLAN.WORK_PLAN_CD) → TB_SCH_MGMT 조인.
 * 요청 스케줄(req*) = REQ.SCH_CD → TB_SCH_MGMT 조인. 미처리 대기 건이므로
 * cur* 가 변경 전(승인 전 현재), req* 가 변경 후(승인 시 반영될 값)에 해당한다.
 *
 * <p>현재 근무계획이 없거나(미래 신규) WORK_PLAN_CD 가 LEAVE_CD 이면 cur* 는 NULL.
 * 시각(*StrTime/*EndTime)은 HHMM 원시 문자열이며, 서비스에서 fmtHm 으로 표시 변환한다.
 */
public record SchedBodyRow(
      String curSchCd
    , String curSchNo
    , String curFstStrTime
    , String curFstEndTime
    , String curSecStrTime
    , String curSecEndTime
    , String reqSchCd
    , String reqSchNo
    , String reqFstStrTime
    , String reqFstEndTime
    , String reqSecStrTime
    , String reqSecEndTime

    // PRAFTA-FIXEDOT-2: 현재/요청 스케줄의 고정연장(전방·후방, HHmm, NULL=없음) — 상세 비교 표기용.
    //   ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지).
    , String curPreFixedOtStrTime
    , String curPreFixedOtEndTime
    , String curFixedOtStrTime
    , String curFixedOtEndTime
    , String reqPreFixedOtStrTime
    , String reqPreFixedOtEndTime
    , String reqFixedOtStrTime
    , String reqFixedOtEndTime
) {
}
