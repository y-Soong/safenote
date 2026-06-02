package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E6 위험성평가요청 -> 아차사고 재분류 요청 (설계문서 4-B).
 * srcProcessCd/srcAssessmentCd 로 원 tb_risk_assessment 건을 지정.
 * 신규 tb_near_miss 의 사건 속성(유형/발생일시/장소/경위/잠재중대성)을 함께 입력.
 * siteCd 는 원 건과 신규 건의 사업장. cmpnyCd/reporter 는 JWT 에서만 도출.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReclassifyRequest {
    private String siteCd;
    private String srcProcessCd;
    private String srcAssessmentCd;

    private String incidentTypeCd;
    private String processCd;
    private String occurDtime; // YYYY-MM-DD HH:mm
    private String locationDesc;
    private String description;
    private String potentialSeverityCd;
    private String immediateActionDesc;
}
