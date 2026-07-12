package com.prafta.web.risk.riskai01.application.model;

/**
 * AI 도출 입력 원천(tb_risk_assessment 조회 결과 + 구조화 값 조인). 존재하지 않으면 매퍼가 null 반환(→ 404).
 *
 * <p>⚠️ MyBatis record 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서
 *    (INIT_DESC, ASSESSMENT_DESC, INIT_FILE_MGMT_CD, PROCESS_NM, RISK_TYPE_NM, HAZARD_NM).
 *
 * <p>구조화 값(도출 프롬프트 입력): 공정명(=화면 "위험성구분"/"작업명")·위험성 분류명·유해요인명은
 *    Risk03Mapper 조인(COM002/TB_RISK_TYPE/TB_RISK_SITE_HAZARD)으로 회사 스코프 하에 파생한다.
 */
public record RiskAssessmentAiSource(
    String initDesc         // INIT_DESC (유해요인 설명)
    , String assessmentDesc // ASSESSMENT_DESC (유해요인 직접입력)
    , String initFileMgmtCd // INIT_FILE_MGMT_CD (유해요인 사진 파일코드; 없을 수 있음)
    , String processNm      // 공정명(COM002 BAIM_VAL_D_NM) = 화면 위험성구분/작업명
    , String riskTypeNm     // 위험성 분류명(TB_RISK_TYPE.RISK_TYPE_NM)
    , String hazardNm       // 유해요인명(self→ASSESSMENT_DESC, 그 외 TB_RISK_SITE_HAZARD.HAZARD_NM)
) {}
