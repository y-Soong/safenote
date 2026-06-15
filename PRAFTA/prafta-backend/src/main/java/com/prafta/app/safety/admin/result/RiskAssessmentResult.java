package com.prafta.app.safety.admin.result;

/**
 * H3/H4 위험성평가 목록/상세 행 VO (tb_risk_assessment 기준, camelCase 매핑).
 *
 * <p>웹 risk03 selectRiskAssessmentsLists 를 앱에 포팅하되, 사업장 스코프를 강제하고
 *    개선 후 풀 재평가(REVAL_*) 표시 컬럼까지 읽기로 노출한다(모바일은 입력 안 함, 표시만).
 *    사진 경로(initFilePath/revalFilePath)는 FNC_CMM_INFO_SRCH ... 'FILE_PATH' 로 해석.
 *
 * <p>매핑은 위치 기반(SELECT 컬럼 순서 = 생성자 인자 순서) — 순서 변경 시 SQL 동기 필수.
 */
public record RiskAssessmentResult(
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
    , String initFileName
    , String revalDate
    , String revalBeforeDesc
    , Integer revalLikelihoodScore
    , Integer revalSeverityScore
    , String revalRiskLv
    , String revalDesc
    , String revalAssessorId
    , String revalAssessorNm
    , String revalAssessDate
){
}
