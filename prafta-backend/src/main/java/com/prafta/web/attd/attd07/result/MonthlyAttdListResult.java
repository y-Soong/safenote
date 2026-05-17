package com.prafta.web.attd.attd07.result;

public record MonthlyAttdListResult(
      /* ── 사용자 정보 (사용자별 동일) ───────────── */
      String userCd
    , String userId
    , String userNm
    , String deptNm
    , String authCd
    , String authNm

      /* ── INSERT용 키 (셀별로 박혀있어야 함) ───── */
    , String cmpnyCd
    , String siteCd
    , String nodeCd
    , String workYmd          // YYYYMMDD

      /* ── 일자 정보 ─────────────────────────── */
    , String dow             // 0=일 ~ 6=토

      /* ── 근무계획 ───────────────────────────── */
    , String workPlanCd       // SCH_CD 또는 LEAVE_CD
    , String planType         // SCH | LEAVE | null

      /* ── 스케줄 정보 (1·2구간) ──────────────── */
    , String schType          // 01(1구간) | 02(2구간)
    , String plan1Start       // HHmm
    , String plan1End
    , String plan1BreakMin
    , String plan2Start
    , String plan2End
    , String plan2BreakMin

      /* ── 휴가 정보 ──────────────────────────── */
    , String leaveNm

      /* ── 근태 1차 ──────────────────────────── */
    , String attd1Id
    , String act1InDate
    , String act1InTime
    , String act1InStdTime
    , String act1InMethod
    , String act1OutDate
    , String act1OutTime
    , String act1OutStdTime
    , String act1OutMethod

      /* ── 근태 2차 ──────────────────────────── */
    , String attd2Id
    , String act2InDate
    , String act2InTime
    , String act2InStdTime
    , String act2InMethod
    , String act2OutDate
    , String act2OutTime
    , String act2OutStdTime
    , String act2OutMethod
) {
}
