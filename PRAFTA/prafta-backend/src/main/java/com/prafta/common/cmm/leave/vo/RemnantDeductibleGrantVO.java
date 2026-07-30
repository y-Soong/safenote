package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * PC-05(D6): 짜투리 발동/회수 차감 대상 부여 1행 — 대상 5종 활성 부여(잔여&gt;0, 만료 임박순).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 * {@code LeaveRemnantCoverMapper.selectRemnantDeductibleGrants} 의 SELECT 절과 1:1.
 */
public record RemnantDeductibleGrantVO(
      String grantId
    , String leaveCd
    , BigDecimal grantDays
    , BigDecimal usedDays
) {
}
