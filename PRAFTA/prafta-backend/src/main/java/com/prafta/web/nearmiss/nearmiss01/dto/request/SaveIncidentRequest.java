package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E4 정밀조사 저장 요청 (원인/재발방지/임시조치).
 * 식별(cmpnyCd/reviewer)은 JWT 에서만 도출.
 */
@Getter
@Setter
@NoArgsConstructor
public class SaveIncidentRequest {
    private String siteCd;
    private String nearMissId;
    private String causeDesc;
    private String preventionDesc;
    private String immediateActionDesc;
}
