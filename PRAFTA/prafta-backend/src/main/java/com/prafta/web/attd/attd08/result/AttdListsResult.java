package com.prafta.web.attd.attd08.result;

public record AttdListsResult(
    /* user info */
      String attdId
    , String userCd
    , String userId
    , String userNm
    , String nodeCd
    , String nodeNm
    , String cmpnyCd
    , String siteCd
    , String workYmd
    , Integer workSeq

    /* schedule (1st / 2nd block) */
    , String schType
    , String plan1Start
    , String plan1End
    , String plan1BreakMin
    , String plan2Start
    , String plan2End
    , String plan2BreakMin

    /* actual 1st */
    , String act1InDate
    , String act1InTime
    , String act1InMethod
    , String act1OutDate
    , String act1OutTime
    , String act1OutMethod

    /* actual 2nd */
    , String act2InDate
    , String act2InTime
    , String act2InMethod
    , String act2OutDate
    , String act2OutTime
    , String act2OutMethod

    /* outside / status */
    , String isOutsideYn
    , String attdStatusCd

    /* row type discriminator (PRAFTA-015) */
    , String rowType   /* 'NORMAL' | 'OT' */
    , String otId      /* 초과근무 ID — OT 행만, NORMAL 은 NULL */

    /* 초과근무 인정시간(관리자 승인 실근무 분, 휴게 제외) — OT 행만, NORMAL 은 NULL */
    , Integer otWorkMinutes

    /**
     * HB-05(D1): 그 행(차수 구간)의 <b>반차 반영 유효 소정 시작</b>(HHmm).
     * 그날 확정 반차가 없으면 원 스케줄 시각과 같고, 구간 전체가 면제되면 null(판정 제외).
     * 매퍼는 원 스케줄 시각을 싣고 서비스가 {@code PartialLeaveWindowUtils} 결과로 덮어쓴다.
     * 화면(Attd_08.vue)은 지각·조퇴 재계산에 이 값을 쓴다(클라이언트 규칙 재구현 금지).
     */
    , String effPlanStart

    /** HB-05(D1): 그 행(차수 구간)의 반차 반영 유효 소정 종료(HHmm). 구간 전체 면제면 null. */
    , String effPlanEnd

    /* ────────────────────────────────────────────────────────────────
       PRAFTA-FIXEDOT-3: 고정연장(전방·후방) 표기 + "연장 미이행" 배지(정책 ② — 조퇴와 완전 분리).
       ⚠️ record 끝 = SELECT 끝 동일 순서(위치 매핑, UNION 양 분기 동수). OT 행은 전부 NULL.
       ──────────────────────────────────────────────────────────────── */

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
     * 'Y' 면 고정연장 의무 면제(미이행 배지 발화 금지 — 단 실적 계상은 그대로).
     */
    , String fixedOtExemptYn
    /**
     * "연장 미이행" 배지('Y'/null) — 서비스 파생 판정(저장 없음). 그날 마지막 스케줄 슬롯 행에만 'Y'.
     * 조건: 후방 고정연장 존재 + 마지막 슬롯 퇴근 완료 + 퇴근 스탬프 &lt; 후방 종료 + 연차 계열 미사용.
     */
    , String fixedOtUnfulfilledYn
    /**
     * 그날 고정연장 실적(분, 실근태 ∩ 고정연장 — FixedOtMinutesUtils 파생 계산). 그날 마지막
     * 슬롯 행에만 실리며(일 단위 값의 행 중복 방지) 고정연장 없는 타입은 null.
     */
    , Integer fixedOtActMinutes

    /**
     * 고용형태(TB_USER.EMPLOYMENT_TYPE) — 'REGULAR'/'DAILY'/NULL.
     * 화면(Attd_08)의 정규직/일용직 구분 컬럼용. NULL 은 정규직 취급(레거시 행 — DAILY 아님).
     * ⚠️ record 끝 = SELECT 끝 동일 순서(위치 매핑) — UNION 양 분기 끝에 동수로 추가되어 있다.
     */
    , String employmentType

    /* ────────────────────────────────────────────────────────────────
       BW-05(2026-09-04): 스케줄 휴게 시각(1·2구간). 화면 인정시간/실근로가 "실제로 쉰 휴게만 공제"
       (휴게 시각 ∩ 실근태 ∩ 유효 소정) 교집합을 계산하는 입력. 시각 없는(분만) 타입은 null → 종전 분 공제.
       ⚠️ record 끝 = SELECT 끝 동일 순서(위치 매핑, UNION 양 분기 동수). OT 행은 전부 NULL.
       ──────────────────────────────────────────────────────────────── */
    /** 1구간 휴게 시작(HHMM, 없으면 null). */
    , String plan1BrkStr
    /** 1구간 휴게 종료(HHMM 또는 '2400', 없으면 null. 종료 &lt; 시작이면 자정 넘김). */
    , String plan1BrkEnd
    /** 2구간 휴게 시작(HHMM, 없으면 null). */
    , String plan2BrkStr
    /** 2구간 휴게 종료(HHMM 또는 '2400', 없으면 null). */
    , String plan2BrkEnd
) {

    /**
     * D-1: 서비스가 Java 로 산출한 판정 결과(상태 + 유효 소정 시각)를 반영한 새 인스턴스.
     * record 는 불변이므로 교체 생성한다(컴포넌트 순서 변경 시 이 메서드도 함께 수정할 것).
     */
    public AttdListsResult withJudgement(String newAttdStatusCd, String newEffPlanStart, String newEffPlanEnd) {
        return new AttdListsResult(
              attdId, userCd, userId, userNm, nodeCd, nodeNm, cmpnyCd, siteCd, workYmd, workSeq
            , schType, plan1Start, plan1End, plan1BreakMin, plan2Start, plan2End, plan2BreakMin
            , act1InDate, act1InTime, act1InMethod, act1OutDate, act1OutTime, act1OutMethod
            , act2InDate, act2InTime, act2InMethod, act2OutDate, act2OutTime, act2OutMethod
            , isOutsideYn, newAttdStatusCd
            , rowType, otId, otWorkMinutes
            , newEffPlanStart, newEffPlanEnd
            , preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
            , fixedOtExemptYn, fixedOtUnfulfilledYn, fixedOtActMinutes
            , employmentType
            , plan1BrkStr, plan1BrkEnd, plan2BrkStr, plan2BrkEnd
        );
    }

    /**
     * PRAFTA-FIXEDOT-3: 고정연장 파생 산출(미이행 배지 + 실적 분)을 반영한 새 인스턴스.
     * 판정(attdStatusCd)·유효 소정 시각에는 손대지 않는다(조퇴 통계와 완전 분리).
     */
    public AttdListsResult withFixedOt(String newFixedOtUnfulfilledYn, Integer newFixedOtActMinutes) {
        return new AttdListsResult(
              attdId, userCd, userId, userNm, nodeCd, nodeNm, cmpnyCd, siteCd, workYmd, workSeq
            , schType, plan1Start, plan1End, plan1BreakMin, plan2Start, plan2End, plan2BreakMin
            , act1InDate, act1InTime, act1InMethod, act1OutDate, act1OutTime, act1OutMethod
            , act2InDate, act2InTime, act2InMethod, act2OutDate, act2OutTime, act2OutMethod
            , isOutsideYn, attdStatusCd
            , rowType, otId, otWorkMinutes
            , effPlanStart, effPlanEnd
            , preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
            , fixedOtExemptYn, newFixedOtUnfulfilledYn, newFixedOtActMinutes
            , employmentType
            , plan1BrkStr, plan1BrkEnd, plan2BrkStr, plan2BrkEnd
        );
    }
}
