package com.prafta.web.risk.riskimpr01.result;

/**
 * 개선항목 1건 결과 VO (tb_risk_improvement_item 기준, camelCase 매핑).
 * 사진은 FILE_MGMT_CD 와 파일명/경로(FNC_CMM_INFO_SRCH 해석)를 함께 보유한다.
 */
public record ImprovementItemResult(
    Integer improvementSeq
    , String improveDate
    , String improveDesc
    , String fileMgmtCd
    , String fileName
    , String filePath
    , Integer likelihoodScore
    , Integer severityScore
    , String riskLv
){
}
