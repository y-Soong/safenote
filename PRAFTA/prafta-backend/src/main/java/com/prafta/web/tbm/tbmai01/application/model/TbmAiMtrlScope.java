package com.prafta.web.tbm.tbmai01.application.model;

/**
 * 자료 단위 사업장 스코프 조회 결과(analyze-items · analysis-status 진입 게이트용).
 *
 * <p>회사 소유(CMPNY_CD) + 사업장(SITE_CD)을 한 번에 도출한다. 미소유/미존재면 매퍼가
 *    null 을 반환한다(→ 호출부 AI_404_002 존재 비노출). 조회된 {@code siteCd} 로
 *    사업장 격리 게이트({@code assertSiteAccess}/{@code hasSiteAccess})를 수행한다.
 *
 * <p>★MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서(MTRL_CD, SITE_CD).
 */
public record TbmAiMtrlScope(
    String mtrlCd    // MTRL_CD (교육자료 코드, PK)
    , String siteCd  // SITE_CD (비어있음=회사공통)
) {}
