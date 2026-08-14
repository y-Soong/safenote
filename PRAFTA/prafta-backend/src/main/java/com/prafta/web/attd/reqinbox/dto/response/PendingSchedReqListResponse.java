package com.prafta.web.attd.reqinbox.dto.response;

import java.util.List;

import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;

import lombok.Builder;
import lombok.Value;

/**
 * 요청 승인 관리 — 스케줄 수정('10') 대기요청 목록 응답.
 *
 * <p>필드명은 {@link PendingReqListResponse} 와 동일하게 {@code pendingList} 를 유지한다.
 * 프론트는 두 경우 모두 {@code r.data.pendingList} 로 읽으므로 응답 계약이 동일하다.
 */
@Value
@Builder
public class PendingSchedReqListResponse {
    List<PendingSchedReqResult> pendingList;
}
