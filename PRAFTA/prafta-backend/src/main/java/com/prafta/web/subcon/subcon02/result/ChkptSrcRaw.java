package com.prafta.web.subcon.subcon02.result;

/**
 * 미러 복제 원본 점검대상 1행(PRAFTA-SUBCON-T6-02).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ChkptSrcRaw(
    String chkLstType
    , String chkptCd
    , String chkptNm
    , String chkptDesc
    , String useYn
){
}
