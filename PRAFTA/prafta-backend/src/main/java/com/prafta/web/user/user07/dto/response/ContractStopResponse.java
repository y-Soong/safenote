package com.prafta.web.user.user07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 사용중지 응답 (POST /webApi/user07/contract-stop).
 */
@Value
@Builder
public class ContractStopResponse {
    int processedCount;
}
