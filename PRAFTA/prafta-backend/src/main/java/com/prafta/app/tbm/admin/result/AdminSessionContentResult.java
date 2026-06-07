package com.prafta.app.tbm.admin.result;

/** 세션 상세 - 연계 콘텐츠 묶음 매핑 + 묶음 정보. */
public record AdminSessionContentResult(
    String mtrlCd
    , String title
    , String mtrlType
    , String mtrlTypeNm
    , int itemCnt
    , int displayOrder
    , String overrideDesc
){
}
