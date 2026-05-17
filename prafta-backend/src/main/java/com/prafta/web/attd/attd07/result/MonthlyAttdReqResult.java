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
) {
}
