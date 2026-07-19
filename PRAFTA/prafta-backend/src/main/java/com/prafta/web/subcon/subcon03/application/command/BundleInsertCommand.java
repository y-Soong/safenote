package com.prafta.web.subcon.subcon03.application.command;

/**
 * 릴레이 묶음 감사 INSERT 커맨드(PRAFTA-SUBCON-T3 §5-7).
 *
 * <p>제공측 테넌트 전용 기록이다(ownerCmpnyCd = 승인자 gv_cmpnyCd).
 * 수신측 조회 API 는 이 테이블을 절대 조인하지 않는다(하위 회사 비노출).
 */
public record BundleInsertCommand(
    Long snapshotId
    , Long includedRcvSnapshotId
    , String ownerCmpnyCd
    , int rowCnt
    , String insertNo
){
}
