package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * PC-06(D7): 회수 판정 대상 COVER 1행(ACTIVE).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 * {@code LeaveRemnantCoverMapper.selectActiveCovers} 의 SELECT 절과 1:1.
 */
public record RemnantCoverRowVO(
      String coverId
    , String reqId
    , String siteCd
    , String workYmd
    , String useUnitType
    , BigDecimal chargeDays
    , BigDecimal remnantDays
    , BigDecimal coverDays
    , Integer coverMinutes
    , Integer convMinutes
) {
}
