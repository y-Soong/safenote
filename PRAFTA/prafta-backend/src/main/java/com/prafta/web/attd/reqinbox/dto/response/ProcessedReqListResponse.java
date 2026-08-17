package com.prafta.web.attd.reqinbox.dto.response;

import java.util.List;

import com.prafta.web.attd.reqinbox.result.ProcessedLeaveChangeResult;
import com.prafta.web.attd.reqinbox.result.ProcessedReqResult;

import lombok.Builder;
import lombok.Value;

/**
 * 요청 승인 관리 — 내 처리 이력 응답.
 *
 * <p>{@code leaveChangeList} 는 연차 탭(reqTypeGroup=leave)에서만 채워진다(그 외 빈 목록).
 */
@Value
@Builder
public class ProcessedReqListResponse {
    List<ProcessedReqResult> processedList;
    List<ProcessedLeaveChangeResult> leaveChangeList;
}
