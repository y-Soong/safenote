package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E2 사건 단건 상세 / E3 상태 카운트 조회 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class IncidentInfoRequest {
    private String siteCd;
    private String nearMissId;
}
