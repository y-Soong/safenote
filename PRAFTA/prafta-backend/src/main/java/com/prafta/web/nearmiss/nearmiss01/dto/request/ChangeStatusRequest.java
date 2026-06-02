package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E5 상태 전환 요청 (100->200->300->400, 어디서든 900 반려).
 * 반려(900) 시 rejectReason 필수.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangeStatusRequest {
    private String siteCd;
    private String nearMissId;
    private String reportStatusCd; // 전환 목표 상태
    private String rejectReason;   // 900 반려 시 사유
}
