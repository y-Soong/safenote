package com.prafta.app.tbm.admin.result;

/** R5 교육자료 상세 묶음(마스터). 스코프 검증용 siteCd 포함. */
public record AdminEduMaterialResult(
    String mtrlCd
    , String title
    , String mtrlType
    , String mtrlTypeNm
    , String contents
    , String siteCd
    , String isCommonContent   // 'Y'(회사공통) / 'N'(사업장전용)
    , String useYn
    , String insertNm
    , String insertDate
){
}
