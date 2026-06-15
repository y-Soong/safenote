package com.prafta.web.attd.attd13.result;

import java.math.BigDecimal;

/**
 * 근로자 본인 이동 가능 연차일 1건 (PRAFTA-COM-008-C, C-5a).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑).
 */
public record MovableLeaveResult(
      String leaveId
    , String leaveCd
    , String startDate
    , BigDecimal leaveDays
    , String useUnitType
    , String promotionStage
    , String designatorType
    , String availToDate
) {
}
