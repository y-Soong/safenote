package com.prafta.web.attd.reqinbox.dto.response;

import java.util.List;

import com.prafta.web.attd.reqinbox.result.PendingReqResult;

import lombok.Builder;
import lombok.Value;

/** 요청 승인 관리 대기요청 목록 응답 (prafta-019 후속). */
@Value
@Builder
public class PendingReqListResponse {
    List<PendingReqResult> pendingList;
}
