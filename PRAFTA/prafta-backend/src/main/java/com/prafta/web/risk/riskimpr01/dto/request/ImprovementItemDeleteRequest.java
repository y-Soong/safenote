package com.prafta.web.risk.riskimpr01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개선항목 삭제(soft delete) 요청.
 * cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class ImprovementItemDeleteRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    private Integer improvementSeq;
}
