package com.prafta.app.tbm.admin.application.query;

/** R5 교육자료 상세(묶음/항목) 조회 Query. 식별자(회사)는 토큰 출처(IDOR 차단). */
public record AdminEduMaterialDetailQuery(
    String mtrlCd
    , String gvCmpnyCd
){
    public static AdminEduMaterialDetailQuery of(String mtrlCd, String gvCmpnyCd) {
        return new AdminEduMaterialDetailQuery(mtrlCd, gvCmpnyCd);
    }
}
