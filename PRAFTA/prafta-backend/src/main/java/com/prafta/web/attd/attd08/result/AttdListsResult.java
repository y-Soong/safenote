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
        );
    }
}
