package com.prafta.web.subcon.subcon03.application.command;

import java.math.BigDecimal;

/**
 * 근태 스냅샷 상세행 INSERT 커맨드 1건(PRAFTA-SUBCON-T3 §5-5).
 *
 * <p>PII 최소수집(D8): 인적 정보는 성명(workerNm) + 소속표시(affilCmpnyNm) 뿐이다.
 * 원본 USER_CD/USER_ID/부서/연락처/기기ID/연차사유는 어떤 필드에도 담지 않는다.
 * workerSeq 는 스냅샷 스코프 로컬 번호(그룹핑 전용)다.
 */
public record SnapshotAttdInsertCommand(
    Long snapshotId
    , int workerSeq
    , String workerType
    , String workerNm
    , String affilCmpnyNm
    , String rowType
    , String workYmd
    , int workSeq
    , String schNm
    , String schType
    , String planStrTime
    , String planEndTime
    , Integer planBrkMin
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
    , String attdStatusCd
    , int otMinutes
    , String leaveNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String leaveEndYmd
    , String insertNo
){
}
