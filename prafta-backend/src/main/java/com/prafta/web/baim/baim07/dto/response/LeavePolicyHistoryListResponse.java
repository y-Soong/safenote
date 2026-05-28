package com.prafta.web.baim.baim07.dto.response;

import com.prafta.common.cmm.leave.vo.LeavePolicyHistoryVO;
import com.prafta.common.cmm.leave.vo.PagedResult;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 변경 이력 페이징 응답.
 */
@Value
@Builder
public class LeavePolicyHistoryListResponse {

    PagedResult<LeavePolicyHistoryVO> history;
}
