package com.prafta.web.attd.attd07.result;

public record DailyAttdDetailsResult(
    /* 사용자/사업장 기본 정보 */
      String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String employmentType   // 고용형태: DAILY(일용직) / REGULAR(정규직) / null(미지정=정규직 취급)
    , String siteCd
    , String nodeCd
    , String workYmd

    /* 스케줄 정보 (1·2구간) */
    , String schType
    , String plan1Start
    , String plan1End
    , Integer plan1BreakMin
    , String plan2Start
    , String plan2End
    , Integer plan2BreakMin

    /* 근태 1차 */
    , String attd1Id
    , String act1InDate
    , String act1InTime
    , String act1InMethod
    , String act1OutDate
    , String act1OutTime
    , String act1OutMethod

    , String oriAct1InDate
    , String oriAct1InTime
    , String oriAct1OutDate
    , String oriAct1OutTime

    /* 근태 2차 */
    , String attd2Id
    , String act2InDate
    , String act2InTime
    , String act2InMethod
    , String act2OutDate
    , String act2OutTime
    , String act2OutMethod
    
    , String oriAct2InDate
    , String oriAct2InTime
    , String oriAct2OutDate
    , String oriAct2OutTime

    /* PRAFTA-011 - 구간별 외근 플래그.
       해당 구간 ATTD_ID 에 연결된 TB_USER_ATTD_GPS 행이 1건이라도 존재하면 'Y',
       없으면 'N'. GPS 테이블에는 WORK_SEQ 컬럼이 없으므로 구간 구분은 ATTD_ID 로만 한다. */
    , String attd1OutsideYn
    , String attd2OutsideYn

    /* PRAFTA-FIXEDOT-3 - 고정연장근무 표기 + "연장 미이행" 배지(정책 ②) + 실적(정책 ①).
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
    /** "연장 미이행" 배지('Y'/null) — 서비스 파생 판정(저장 없음). 조퇴 판정과 완전 분리. */
    , String fixedOtUnfulfilledYn
    /** 그날 고정연장 실적(분, 실근태 ∩ 고정연장 — FixedOtMinutesUtils 파생). 고정연장 없는 타입은 null. */
    , Integer fixedOtActMinutes

    /* 2026-08-17: 휴게 시각(스케줄 설정값) — 일자상세 팝업 근무계획 휴게 표기 전용(additive).
       ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지). */

    /** 1구간 휴게 시작(HHMM, 미설정 null). */
    , String fstBrkStrTime
    /** 1구간 휴게 종료(HHMM). */
    , String fstBrkEndTime
    /** 2구간 휴게 시작(HHMM, 미설정 null). */
    , String secBrkStrTime
    /** 2구간 휴게 종료(HHMM). */
    , String secBrkEndTime

    /* [소속이동이력가시성-03] "현재 타 사업장 소속" 배지용(신규). TB_USER B(이미 조인) 의
       B.SITE_CD 가 곧 "현재 소속" — 조회 사업장(siteCd)과 다르면 이동 전 이력을 보고 있는 것. */
    /** 근로자의 현재 소속 사업장코드(TB_USER.SITE_CD). */
    , String currentSiteCd
    /** 근로자의 현재 소속 사업장명. */
    , String currentSiteNm
) {

    /**
     * PRAFTA-FIXEDOT-3: 고정연장 파생 산출(미이행 배지 + 실적 분)을 반영한 새 인스턴스.
     * 판정·스케줄·근태 필드에는 손대지 않는다(조퇴 통계와 완전 분리).
     */
    public DailyAttdDetailsResult withFixedOt(String newFixedOtUnfulfilledYn, Integer newFixedOtActMinutes) {
        return new DailyAttdDetailsResult(
              cmpnyCd, userCd, userId, userNm, employmentType, siteCd, nodeCd, workYmd
            , schType, plan1Start, plan1End, plan1BreakMin, plan2Start, plan2End, plan2BreakMin
            , attd1Id, act1InDate, act1InTime, act1InMethod, act1OutDate, act1OutTime, act1OutMethod
            , oriAct1InDate, oriAct1InTime, oriAct1OutDate, oriAct1OutTime
            , attd2Id, act2InDate, act2InTime, act2InMethod, act2OutDate, act2OutTime, act2OutMethod
            , oriAct2InDate, oriAct2InTime, oriAct2OutDate, oriAct2OutTime
            , attd1OutsideYn, attd2OutsideYn
            , preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
            , fixedOtExemptYn, newFixedOtUnfulfilledYn, newFixedOtActMinutes
            , fstBrkStrTime, fstBrkEndTime, secBrkStrTime, secBrkEndTime
            , currentSiteCd, currentSiteNm
        );
    }
}
