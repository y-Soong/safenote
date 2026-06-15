package com.prafta.web.leave.promotion.leavepromo01.result;

import java.math.BigDecimal;

/**
 * prafta-com-008-A-4: 2차 회사직권 대상자 1행 결과 (조회 테이블).
 *
 * <p>★ MyBatis record 매핑 — SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반, 메모리
 * feedback_mybatis_record_column_order). 컬럼 추가/삭제 시 SELECT 도 동일 위치로 맞출 것.
 *
 * <p>미사용 연차(unusedDays) = 본연차+근속가산 ACTIVE (GRANT_DAYS-USED_DAYS) 합(미래 등록분=사용 간주).
 * 사번 컬럼은 tb_user 에 없으므로(확정-4) USER_CD/이름/부서만 표시한다.
 */
public record PromotionTargetRowResult(
        String userCd,
        String userNm,
        String nodeNm,
        String siteNm,
        String hireDate,
        BigDecimal unusedDays,
        BigDecimal stage2TargetDays,
        String baseAvailToDate
) {
}
