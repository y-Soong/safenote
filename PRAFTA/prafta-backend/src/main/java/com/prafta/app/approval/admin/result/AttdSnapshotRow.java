package com.prafta.app.approval.admin.result;

/**
 * 001-P2-B3: 근태보정 상세 Before(원본 출퇴근) 스냅샷(web Attd07Mapper.selectAttdSnapshotById 포팅).
 */
public record AttdSnapshotRow(
      String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
) {
}
