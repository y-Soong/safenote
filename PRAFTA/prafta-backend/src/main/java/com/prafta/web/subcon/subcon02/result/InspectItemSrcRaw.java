package com.prafta.web.subcon.subcon02.result;

/**
 * 미러 복제 원본 점검문항 1행(PRAFTA-SUBCON-T6-02).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record InspectItemSrcRaw(
    String chkLstType
    , String inspectItemCd
    , String inspectItemSubj
    , Integer sortIdx
    , String strDate
    , String useYn
){
}
