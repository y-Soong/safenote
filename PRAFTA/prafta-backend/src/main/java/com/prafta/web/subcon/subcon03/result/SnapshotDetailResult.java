package com.prafta.web.subcon.subcon03.result;

import java.math.BigDecimal;

/**
 * 수신 스냅샷 상세행 1건(읽기전용 — PRAFTA-SUBCON-T3 §5-8).
 *
 * <p>응답에 원본 USER_CD·하위 회사 식별자·릴레이 묶음(BUNDLE) 정보는 존재하지 않는다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SnapshotDetailResult(
    Long detailId
    , Integer workerSeq
    , String workerType
    , String workerNm
    , String affilCmpnyNm
    , String rowType
    , String workYmd
    , Integer workSeq
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
    , Integer otMinutes
    , String leaveNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String leaveEndYmd
){
}
