package com.prafta.app.attd.attd01.result;

/**
 * prafta-app check-out: 셀프 퇴근 대상 "열린 근태" 조회 결과 (TB_USER_ATTD_MGMT).
 *
 * <p>열린 근태 = DEL_YN='N' && CHECK_IN_TIME 有 && CHECK_OUT_TIME NULL.
 *   D+1 윈도우/사업장 검증은 ServiceImpl 에서 수행하므로 판정에 필요한 원천값만 담는다.
 */
public record OpenAttdResult(
    String attdId
    , String workYmd
    , int workSeq
    , String siteCd
    , String nodeCd
) {
}
