package com.prafta.app.tbm.admin.result;

/** R5 교육자료 묶음 리스트 행(탭3). isCommonContent = SITE_CD IS NULL 여부. */
public record AdminEduMaterialListResult(
    String mtrlCd
    , String title
    , String mtrlType
    , String mtrlTypeNm
    , String siteCd
    , String isCommonContent   // 'Y'(회사공통) / 'N'(사업장전용)
    , String contents          // 설명
    , int itemCnt
    , String useYn
    , String insertNm
    , String insertDate
){
}
