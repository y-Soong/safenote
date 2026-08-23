package com.prafta.web.attd.attd07.result;

public record MonthlyAttdListResult(
      /* ── 사용자 정보 (사용자별 동일) ───────────── */
      String userCd
    , String userId
    , String userNm
    , String deptNm
    , String authCd
    , String authNm
    , String employmentType   // 고용형태: DAILY(일용직) / REGULAR(정규직) / null(미지정=정규직 취급)

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
    , String act1InMethod
    , String act1OutDate
    , String act1OutTime
    , String act1OutMethod

      /* ── 근태 2차 ──────────────────────────── */
    , String attd2Id
    , String act2InDate
    , String act2InTime
    , String act2InMethod
    , String act2OutDate
    , String act2OutTime
    , String act2OutMethod

      /* ── PRAFTA-017 - 구간별 외근 플래그 ───────
         구간 ATTD_ID 가 TB_USER_ATTD_GPS 에 존재하면 'Y', 없으면 'N'. */
    , String attd1OutsideYn
    , String attd2OutsideYn

      /* ── PRAFTA-FIXEDOT-3 - 고정연장근무 표기 + "연장 미이행" 배지(정책 ②) ──
         ⚠️ record 끝 = SELECT 끝 동일 순서(MyBatis 위치 기반 매핑, 중간 삽입 금지). */

      /** 전방 고정연장 시작(HHMM, 없으면 null) — 표기 전용. */
    , String preFixedOtStrTime
      /** 전방 고정연장 종료(HHMM). */
    , String preFixedOtEndTime
      /** 후방 고정연장 시작(HHMM, 없으면 null). */
    , String fixedOtStrTime
      /** 후방 고정연장 종료(HHMM, 종료<=시작이면 자정 넘김 +1440 해석). */
    , String fixedOtEndTime
      /**
       * 그날 연차 계열(종일/반차/시간차) 확정 사용 존재 여부('Y'/'N') — 정책 ③ 존재 검사.
       * 'Y' 면 고정연장 의무 면제(미이행 배지 발화 금지 — 단 실적 계상은 그대로 동작).
       */
    , String fixedOtExemptYn
      /**
       * "연장 미이행" 배지('Y'/null) — 서비스 파생 판정(저장 없음).
       * 조건: 후방 고정연장 존재 + 마지막 스케줄 슬롯 퇴근 완료 + 퇴근 스탬프 &lt; 후방 종료 + 연차 계열 미사용.
       * 조퇴(판정/통계)와 어떤 상태도 공유하지 않는다.
       */
    , String fixedOtUnfulfilledYn
      /** 그날 고정연장 실적(분, 실근태 ∩ 고정연장 — FixedOtMinutesUtils 파생). 고정연장 없는 타입은 null. */
    , Integer fixedOtActMinutes

      /* ── [소속이동이력가시성-01] "현재 타 사업장 소속" 배지용(신규) ──
         조회 사업장(siteCd)과 근로자의 현재 소속이 다를 때(이동자)만 값이 다르다. */
      /** 근로자의 현재 소속 사업장코드(TB_USER.SITE_CD). */
    , String currentSiteCd
      /** 근로자의 현재 소속 사업장명. */
    , String currentSiteNm
) {

    /**
     * PRAFTA-FIXEDOT-3: 고정연장 파생 산출(미이행 배지 + 실적 분)을 반영한 새 인스턴스.
     * 판정·스케줄·근태 필드에는 손대지 않는다(조퇴 통계와 완전 분리).
     */
    public MonthlyAttdListResult withFixedOt(String newFixedOtUnfulfilledYn, Integer newFixedOtActMinutes) {
        return new MonthlyAttdListResult(
              userCd, userId, userNm, deptNm, authCd, authNm, employmentType
            , cmpnyCd, siteCd, nodeCd, workYmd
            , dow
            , workPlanCd, planType
            , schType, plan1Start, plan1End, plan1BreakMin, plan2Start, plan2End, plan2BreakMin
            , leaveNm
            , attd1Id, act1InDate, act1InTime, act1InMethod, act1OutDate, act1OutTime, act1OutMethod
            , attd2Id, act2InDate, act2InTime, act2InMethod, act2OutDate, act2OutTime, act2OutMethod
            , attd1OutsideYn, attd2OutsideYn
            , preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
            , fixedOtExemptYn, newFixedOtUnfulfilledYn, newFixedOtActMinutes
            , currentSiteCd, currentSiteNm
        );
    }
}
