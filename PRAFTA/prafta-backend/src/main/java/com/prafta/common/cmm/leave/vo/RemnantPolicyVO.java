package com.prafta.common.cmm.leave.vo;

/**
 * PC-05(D3): 활성 정책의 짜투리 보전 옵션 + 사용 단위(최소단위 판정 입력).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 * {@code LeaveRemnantCoverMapper.selectRemnantPolicy} 의 SELECT 절과 1:1.
 */
public record RemnantPolicyVO(
      String allowRemnantRoundUp
    , String usageUnit
) {
}
