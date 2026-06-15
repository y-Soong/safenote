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
) {
}
