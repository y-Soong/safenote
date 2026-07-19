package com.prafta.web.subcon.subcon03.application.command;

/**
 * 위험성평가 스냅샷 개선항목 자식행 INSERT 커맨드 1건(PRAFTA-SUBCON-T7 §5-4, 배치).
 *
 * <p>detailId 는 부모 상세행 INSERT 후 회수한 값. fileMgmtCd 는 복제된 수신사 소유 파일코드.
 */
public record SnapshotRiskImproveInsertCommand(
    Long detailId
    , Long snapshotId
    , int improveSeq
    , String improveDate
    , String improveDesc
    , Integer likelihood
    , Integer severity
    , String riskLv
    , String fileMgmtCd
    , String insertNo
){
}
