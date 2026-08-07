package com.prafta.web.subcon.subcon03.result;

import java.math.BigDecimal;

/**
 * 스냅샷 생성 원천행 1건(제공측 근태/초과근무/연차 SELECT 결과 — PRAFTA-SUBCON-T3 §5-5).
 *
 * <p>ATTD / OT_ONLY / LEAVE_ONLY 3종 쿼리가 <b>동일한 컬럼 순서</b>로 이 record 에 매핑된다
 * (MyBatis record 매핑 = SELECT 컬럼 순서 = 컴포넌트 순서 — 순서 변경 금지).
 *
 * <p>userCd 는 <b>서버 내부에서만</b> 사용한다(동의 필터 + WORKER_SEQ 로컬 채번). 스냅샷 상세행에도,
 * 응답에도 저장/노출하지 않는다(D8 최소수집).
 */
public record SnapshotSourceRow(
    String userCd
    , String workerType
    , String workerNm
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
    // [PS-04] 마감 커버리지 필터용 노드 — record 끝 append(SELECT 끝과 순서 일치).
    //   ATTD=A.NODE_CD, OT_ONLY=MIN(O.NODE_CD), LEAVE_ONLY=U.NODE_CD(일용직은 NULL → '*' 전용).
    //   서버 내부 판정 전용 — 스냅샷 상세행/응답에 저장·노출하지 않는다.
    , String nodeCd
){

    /**
     * NF-2a(2026-08-07): 근태 판정({@code attdStatusCd})만 교체한 사본.
     *
     * <p>확정 부분연차(반차)가 있는 날은 SQL CASE 가 아니라 서비스가
     * {@code PartialLeaveWindowUtils} 단일 출처로 재판정한 값을 쓴다.
     * 컴포넌트를 추가·재배치하지 않으므로 MyBatis 위치 매핑에는 영향이 없다.
     */
    public SnapshotSourceRow withAttdStatusCd(String newAttdStatusCd) {
        return new SnapshotSourceRow(
                userCd, workerType, workerNm, rowType, workYmd, workSeq
                , schNm, schType, planStrTime, planEndTime, planBrkMin
                , checkInDate, checkInTime, checkOutDate, checkOutTime
                , newAttdStatusCd, otMinutes, leaveNm, leaveDays, leaveMinutes, leaveEndYmd
                , nodeCd);
    }
}
