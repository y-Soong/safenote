package com.prafta.web.tbm.tbmai02.application.model;

/**
 * 교육안 생성 입력 원천(TB_TBM_SESSION 회사 소유 조회 결과). 없으면 매퍼가 null 반환(→ TBM_404_010).
 *
 * <p>⚠️ MyBatis record 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서
 *    (SESSION_CD, TITLE, SITE_CD, STATUS_CD). 쿼리 컬럼 순서 변경 시 본 순서도 함께 맞춘다.
 */
public record TbmSessionGenSource(
    String sessionCd  // SESSION_CD (세션 코드, PK)
    , String title    // TITLE (세션 제목 = 교육 주제)
    , String siteCd   // SITE_CD (NOT NULL — 세션은 사업장 종속)
    , String statusCd // STATUS_CD (SYS046: DRAFT/OPENED/IN_PROGRESS/COMPLETED/CANCELLED)
) {}
