package com.prafta.web.subcon.subcon03.result;

/**
 * 위험성평가 개선항목 스냅샷 원천행 1건(제공측 tb_risk_improvement_item, USE_YN='Y' — PRAFTA-SUBCON-T7 §5-4).
 *
 * <p>MyBatis record 위치매핑: SELECT 컬럼 순서 = 컴포넌트 순서. {@code processCd/assessmentCd} 는 부모 평가행
 *    그룹핑용 서버 내부 키(미저장). {@code fileMgmtCd} 는 원본 파일코드 — 복제 후 신규 코드로 치환 저장.
 */
public record RiskImproveSourceRow(
    String processCd
    , String assessmentCd
    , Integer improveSeq
    , String improveDate
    , String improveDesc
    , Integer likelihood
    , Integer severity
    , String riskLv
    , String fileMgmtCd
){
}
