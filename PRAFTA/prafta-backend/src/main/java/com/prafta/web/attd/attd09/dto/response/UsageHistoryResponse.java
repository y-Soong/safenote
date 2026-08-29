package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveUsageHistoryRowVO;

import lombok.Builder;
import lombok.Value;

/**
 * 직원별 연도별 연차 사용 이력 응답.
 * GET /attd09/leave-dashboard/{userCd}/usage-history.
 */
@Value
@Builder
public class UsageHistoryResponse {

    /** 조회 연도 (YYYY) */
    String year;

    /** 사용 이력 목록 (dateYmd 오름차순, 일자 전개 완료본) */
    List<LeaveUsageHistoryRowVO> usageHistory;
}
