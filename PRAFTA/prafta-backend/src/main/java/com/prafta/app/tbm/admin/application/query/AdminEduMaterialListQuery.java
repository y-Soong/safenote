package com.prafta.app.tbm.admin.application.query;

import java.util.List;

/**
 * R5 교육자료 리스트 조회 Query(스코프 적용 포함).
 *
 * <p>스코프: companyWide(master/safe)=회사 전체(공통+모든 사업장). 노드관리자=회사공통(SITE_CD IS NULL)
 *   OR 접근가능 사업장(SITE_CD IN accessibleSiteCds). accessibleSiteCds 가 비면 회사공통만.
 */
public record AdminEduMaterialListQuery(
    String mtrlType
    , String title
    , String useYn
    , boolean companyWide
    , List<String> accessibleSiteCds
    , int offset
    , int pageSize
    , String gvCmpnyCd
){
}
