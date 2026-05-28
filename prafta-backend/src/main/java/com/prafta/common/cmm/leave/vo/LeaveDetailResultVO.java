package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 직원별 연차 상세(attd09) 조회 결과 집합(서비스 산출).
 *
 * <p>프론트 계약: {@code { user, legalSummary, nonLegalSummary, grantHistory }}.
 */
@Getter
@Builder
public class LeaveDetailResultVO {

    /** 직원 헤더 정보 */
    private final LeaveDetailUserHeaderVO user;

    /** 법정 휴가 요약 (부여/사용/잔여 + 임박 만료일) */
    private final LeaveSummaryVO legalSummary;

    /** 법정외 휴가 요약 (부여/사용/잔여) */
    private final LeaveSummaryVO nonLegalSummary;

    /** 부여 이력 (GRANT_DATE 내림차순) */
    private final List<LeaveGrantHistoryRowVO> grantHistory;
}
