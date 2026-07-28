package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * PC-07(D9-②): 회사 부담 보전 연간 집계 상세 목록 1행(관리자 화면 — USER_NM 평문 포함).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 * {@code LeaveRemnantCoverMapper.selectCoverSummaryRows} 의 SELECT 절과 1:1.
 */
public record RemnantCoverListRowVO(
      String coverId
    , String userCd
    , String userNm
    , String workYmd
    , String useUnitType
    , BigDecimal chargeDays
    , BigDecimal remnantDays
    , BigDecimal coverDays
    , Integer coverMinutes
    , Integer convMinutes
    , String coverStatus
    , String reqId
    , String insertDate
) {
}
