package com.prafta.web.acct.acct01.result;

/**
 * 확정된 연계 스냅샷 결과 VO (tb_acct_link).
 */
public record LinkSnapshotResult(
    String acctId
    , String linkDomainCd
    , String linkDomainNm
    , Integer linkSeq
    , String linkKeyJson
    , String snapshotJson
    , String insertNo
    , String insertDate
){
}
