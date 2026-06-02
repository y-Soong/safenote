package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveDetailUserHeaderVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantHistoryRowVO;
import com.prafta.common.cmm.leave.vo.LeaveSummaryVO;

import lombok.Builder;
import lombok.Value;

/**
 * 직원별 연차 상세 응답.
 * GET /attd09/leave-dashboard/{userCd}/detail.
 */
@Value
@Builder
public class LeaveDetailResponse {

    /** 직원 헤더 정보 */
    LeaveDetailUserHeaderVO user;

    /** 법정 휴가 요약 */
    LeaveSummaryVO legalSummary;

    /** 법정외 휴가 요약 */
    LeaveSummaryVO nonLegalSummary;

    /** 부여 이력 (GRANT_DATE 내림차순) */
    List<LeaveGrantHistoryRowVO> grantHistory;
}
