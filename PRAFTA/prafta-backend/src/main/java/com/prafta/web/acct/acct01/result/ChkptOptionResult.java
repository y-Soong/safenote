package com.prafta.web.acct.acct01.result;

/**
 * 점검대상(CHKPT) 검색 옵션 결과 VO (tb_chkpt_type_mgmt).
 */
public record ChkptOptionResult(
    String chklstType
    , String chklstTypeNm
    , String chkptCd
    , String chkptNm
    , String mgmtUserCd
    , String mgmtUserNm
){
}
