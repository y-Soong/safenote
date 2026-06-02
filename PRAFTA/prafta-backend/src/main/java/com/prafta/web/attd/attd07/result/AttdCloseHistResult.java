package com.prafta.web.attd.attd07.result;

/**
 * 근태 마감/해제 이력 1행 (tb_attd_close_hist).
 *
 * <p>prafta-019-C 근태 마감 — §13.4 마감 이력 조회.
 */
public record AttdCloseHistResult(
      String histId
    , String closeYm
    , String actionType
    , String actionUserCd
    , String actionDtime
    , String actionDesc
) {
}
