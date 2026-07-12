package com.prafta.web.acct.acct01.result;

/**
 * 위험성평가 개선실행계획서/개선완료보고서 출력용 상세 VO (TB_RISK_ASSESSMENT 단건).
 * FE buildRiskAssessmentData 객체와 1:1 매핑(무가공 전달). risk03 의 상세 컬럼/조인 패턴 재사용.
 * 평가자명은 FNC_CMM_INFO_SRCH(USER_NM), 첨부는 FNC_CMM_INFO_SRCH(FILE_NAME/FILE_PATH) 로 도출한다.
 */
public record RiskAssessmentDetailResult(
    String cmpnyCd
    , String siteCd
    , String processCd
    , String processNm
    , String riskTypeCd
    , String riskTypeNm
    , String hazardCd
    , String hazardNm
    , String assessmentCd
    , String assessmentStatus
    , String assessmentStatusNm
    , Integer initLikelihoodScore
    , Integer initSeverityScore
    , String initRiskLv
    , String initDesc
    , String initAssessorId
    , String initAssessorNm
    , String initAssessDate
    , String initFileMgmtCd
    , String initFilePath
    , String revalDate
    , String revalBeforeDesc
    , Integer revalLikelihoodScore
    , Integer revalSeverityScore
    , String revalRiskLv
    , String revalDesc
    , String revalAssessorId
    , String revalAssessorNm
    , String revalAssessDate
    , String revalFileMgmtCd
    , String revalFilePath
){
}
