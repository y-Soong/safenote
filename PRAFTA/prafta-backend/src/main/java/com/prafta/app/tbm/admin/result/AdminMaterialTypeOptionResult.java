package com.prafta.app.tbm.admin.result;

/** R5 자료 타입(COM003) 옵션. 프론트 타입 필터/셀렉트용. */
public record AdminMaterialTypeOptionResult(
    String code      // BAIM_VAL_D_CD (MTRL_TYPE)
    , String name    // BAIM_VAL_D_NM
){
}
