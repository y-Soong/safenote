package com.prafta.app.approval.admin.result;

/**
 * 앞뒤 근무일(D-1 / D+1) 근태 구간 1행 — 앱 관리자 승인 상세(근태보정) 표시용.
 *
 * <p>{@code AppAdminApprovalMapper.selectNeighborAttdSegments} 결과.
 *   웹 {@code Attd07Mapper.selectAttdSegmentsAroundDayExcept} 의 <b>의도적 미러</b>다
 *   (app 패키지에서 web 매퍼를 주입한 선례가 없어 계층 규약 유지 목적).
 *
 * <p><b>★ record 컴포넌트 순서 = SELECT 컬럼 순서</b>(MyBatis record 생성자 매핑은 컬럼 순서를 따른다).
 */
public record NeighborAttdSegmentRow(
      String workYmd
    , String workSeq
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
) {
}
