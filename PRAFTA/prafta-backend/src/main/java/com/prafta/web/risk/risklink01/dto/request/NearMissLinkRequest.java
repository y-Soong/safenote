package com.prafta.web.risk.risklink01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * L3 연결 추가 / L4 연결 해제 공통 요청.
 * cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class NearMissLinkRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    private String nearMissId;
}
