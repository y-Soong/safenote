package com.prafta.web.attd.attd07.result;

/**
 * tb_attd_close 단건 조회 결과 (회사+사업장+마감월).
 *
 * <p>prafta-019-C 근태 마감. 행이 없으면 미마감(OPEN) 취급.
 */
public record AttdCloseRowResult(
      String closeYm
    , String closeStatus
    , String closeDtime
    , String closeUserCd
    , String uncloseDtime
    , String uncloseUserCd
    , String closeDesc
) {
}
