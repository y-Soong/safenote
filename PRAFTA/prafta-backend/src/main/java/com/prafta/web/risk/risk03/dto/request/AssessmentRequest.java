package com.prafta.web.risk.risk03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssessmentRequest{
	private String siteCd;
	private String assessmentCd;
	private String assessmentStatus;
	private String processCd;

	private String initLikelihoodScore;
	private String initSeverityScore;
	private String initRiskLv;
    
	private String revalDate;
	private String revalBeforeDesc;
	private String revalLikelihoodScore;
	private String revalSeverityScore;
	private String revalRiskLv;
    
	private String revalDesc;

	/** Optional: re-evaluation photo as Base64 (when not using multipart upload). */
	private String itemBase64;
	/** Original filename for {@link #itemBase64} (e.g. photo.jpg). */
	private String itemOriginalFilename;
}
