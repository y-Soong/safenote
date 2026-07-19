package com.prafta.web.subcon.subcon03.result;

/**
 * 위험성평가 스냅샷 생성 원천행 1건(제공측 tb_risk_assessment SELECT 결과 — PRAFTA-SUBCON-T7 §5-4).
 *
 * <p>MyBatis record 위치매핑: SELECT 컬럼 순서 = 컴포넌트 순서(순서 변경 금지).
 *
 * <p>{@code processCd/assessmentCd} 는 개선항목 자식 그룹핑용 <b>서버 내부 키</b>이며 스냅샷/응답에 저장·노출하지
 *    않는다. {@code initAssessorId/revalAssessorId} 는 동의 필터 + ASSESSOR_SEQ 로컬 채번 전용(미저장).
 *    {@code initFileMgmtCd/revalFileMgmtCd} 는 원본(제공사 소유) 파일코드 — 복제 후 신규 코드로 치환해 저장한다.
 *    성명(initAssessorNm/revalAssessorNm)은 원천 평문(FNC_CMM_INFO_SRCH USER_NM) 그대로 복사한다.
 */
public record RiskSourceRow(
    String processCd
    , String assessmentCd
    , String initAssessorId
    , String revalAssessorId
    , String processNm
    , String riskTypeNm
    , String hazardNm
    , String assessmentDesc
    , String assessmentStatusNm
    , String initAssessorNm
    , Integer initLikelihood
    , Integer initSeverity
    , String initRiskLv
    , String initDesc
    , String initAssessDate
    , String initFileMgmtCd
    , String revalAssessorNm
    , Integer revalLikelihood
    , Integer revalSeverity
    , String revalRiskLv
    , String revalDesc
    , String revalAssessDate
    , String revalFileMgmtCd
){
}
