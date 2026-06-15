package com.prafta.app.safety.history.result;

/**
 * 내 안전활동 이력 — 위험성평가 본인 행 VO (prafta-app-025 J1-10 B-6).
 *
 * <p>본인 등록 = TB_RISK_ASSESSMENT.INIT_ASSESSOR_ID = JWT gv_userCd. occurredAt = INIT_ASSESS_DATE.
 *    상태명(SYS011)/공정명(COM002)/위험요인구분명/유해요인명 조인은 J1-6 riskJoins 동형.
 *    사진 경로는 FNC_CMM_INFO_SRCH('FILE_PATH') 로 해석.
 *
 * <p>매핑은 위치 기반(SELECT 컬럼 순서 = 생성자 인자 순서) — 순서 변경 시 SQL 동기 필수.
 */
public record RiskHistoryResult(
      String processCd
    , String processNm
    , String riskTypeCd
    , String riskTypeNm
    , String hazardCd
    , String hazardNm
    , String assessmentCd
    , String assessmentStatus
    , String assessmentStatusNm
    , String initRiskLv
    , String initDesc
    , String initAssessDate     // YYYY-MM-DD HH:mm (표시/정렬용)
    , String occurredDate       // YYYY-MM-DD (병합 정렬 키)
    , String initFileMgmtCd
    , String initFilePath
) {
}
