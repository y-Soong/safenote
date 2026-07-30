package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * PC-07(D9-③): 소멸 임박 짜투리 리포트 — 사용자별 대상 5종 합산 잔여(SQL 집계 원본).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 * {@code LeaveRemnantCoverMapper.selectRemnantRemainByUser} 의 SELECT 절과 1:1.
 */
public record RemnantUserRemainVO(
      String userCd
    , String userNm
    , BigDecimal remnantDays
    , String nearestExpireYmd
) {
}
