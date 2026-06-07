package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * T-K 보조 옵션(콘텐츠/위험성평가) 조회 Query(스코프 적용 포함).
 *
 * <p>master/safe(companyWide=true)=전사. 노드관리자=접근가능 사업장 + (콘텐츠는 회사공통 SITE_CD IS NULL 포함).
 */
public record AdminOptionQuery(
    String siteCd
    , String searchKeyword
    , String processCd
    , boolean companyWide
    , List<String> accessibleSiteCds    // companyWide=false 일 때만 사용
    , String gvCmpnyCd
){
}
