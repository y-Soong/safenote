package com.prafta.web.tbm.tbmai02.application.model;

/**
 * 세션에 묶인 교육자료 세부항목 중 "AI 분석 지정(AI_ANALYZE_YN='Y')됐지만 아직 확정되지 않은" 항목.
 *
 * <p>"확정" = AI_STATUS='CONFIRMED' 且 TRIM(IFNULL(AI_CONFIRM_DESC,'')) 비공백.
 *    그 외(NONE/ANALYZING/DRAFT/FAILED/NULL, 또는 CONFIRMED 인데 확정 서술이 빈 경우) 전부 미확정으로 잡힌다.
 *
 * <p>⚠️ MyBatis record 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서
 *    (M.TITLE, I.MTRL_ITEM_CD, I.MTRL_ITEM_TYPE, I.MTRL_DESC, I.AI_STATUS).
 *    selectSessionUnconfirmedAiItems 쿼리 컬럼 순서 변경 시 본 순서도 함께 맞춘다.
 */
public record TbmUnconfirmedAiItem(
    String mtrlTitle     // M.TITLE (교육자료 제목)
    , String mtrlItemCd  // I.MTRL_ITEM_CD (항목 코드, PK)
    , String mtrlItemType// I.MTRL_ITEM_TYPE (COM003: 01 이미지 / 02 동영상 / 03 유튜브 / 04 PDF)
    , String mtrlDesc    // I.MTRL_DESC (항목 설명, NULL 가능)
    , String aiStatus    // I.AI_STATUS (SYS056: NONE/ANALYZING/DRAFT/FAILED/CONFIRMED, NULL 가능)
) {}
