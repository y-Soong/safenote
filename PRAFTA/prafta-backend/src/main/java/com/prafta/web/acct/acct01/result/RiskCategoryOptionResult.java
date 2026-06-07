package com.prafta.web.acct.acct01.result;

/**
 * 위험성평가 3계층 옵션 결과 VO (공정/위험요인구분/유해요인 공용).
 * categoryType 으로 어느 계층인지 구분(PROCESS/RISK_TYPE/HAZARD).
 */
public record RiskCategoryOptionResult(
    String categoryType // PROCESS / RISK_TYPE / HAZARD
    , String code
    , String name
    , String parentCode // 위험요인구분→공정, 유해요인→위험요인구분
){
}
