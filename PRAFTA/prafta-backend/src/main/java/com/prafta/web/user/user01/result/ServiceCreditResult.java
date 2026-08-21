package com.prafta.web.user.user01.result;

import java.math.BigDecimal;

/**
 * 경력 인정 항목 조회 결과 (PRAFTA-017-4).
 *
 * <p>경력인정 이원화(2026-08-21, 지시서 §1-1)로 leaveCalcYn/extraLeaveDays 2필드 추가.
 * ★record 위치매핑 — User01Mapper.xml selectUserServiceCreditList 의 SELECT 컬럼 순서와
 * 본 생성자 인자 순서가 반드시 일치해야 한다(메모리 feedback_mybatis_record_column_order).
 */
public record ServiceCreditResult(
    String creditId
    , Integer creditMonths
    , String reasonType
    , String reasonDetail
    , String leaveCalcYn
    , BigDecimal extraLeaveDays
) {
}
