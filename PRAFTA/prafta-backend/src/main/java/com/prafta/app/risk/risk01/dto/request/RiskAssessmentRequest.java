package com.prafta.app.risk.risk01.dto.request;

import lombok.Data;

/**
 * prafta-036-B2: 위험성평가 저장(multipart/form-data) 요청.
 * <p>단일 파일은 컨트롤러에서 @RequestPart(value="item") MultipartFile 로 별도 수신.
 * <p>multipart 바인딩 특성상 Lombok @Data 사용(setter 필수).
 */
@Data
public class RiskAssessmentRequest {
    private String siteCd;
    private String processCd;
    private String riskTypeCd;
    private String hazardCd;
    private String assessmentDesc;
    private String initLikelihoodScore;
    private String initSeverityScore;
    private String initRiskLv;
    private String initDesc;
}
