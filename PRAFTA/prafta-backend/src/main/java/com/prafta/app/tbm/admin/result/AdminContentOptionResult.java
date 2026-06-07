package com.prafta.app.tbm.admin.result;

/** 콘텐츠 선택 모달 옵션(콘텐츠 묶음 단위). */
public record AdminContentOptionResult(
    String mtrlCd
    , String title
    , String mtrlType
    , String mtrlTypeNm
    , String siteCd
    , String isCommonContent
    , int itemCnt
){
}
