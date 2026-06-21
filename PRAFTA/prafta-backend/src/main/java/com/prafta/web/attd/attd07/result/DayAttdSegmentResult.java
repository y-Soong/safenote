package com.prafta.web.attd.attd07.result;

/**
 * 같은 근무일 다른 근무 구간(WORK_SEQ)의 출퇴근 시각 — 시각 겹침 판정(정책서 attd §7.6)용 최소 스냅샷.
 *
 * <p>{@code Attd07Mapper.selectDayAttdSegmentsExcept} 결과. 편집/등록 대상 ATTD_ID 를 제외한
 *   같은 (회사/사업장/사용자/근무일) 활성(DEL_YN='N') 근태행의 출퇴근 시각만 담는다.
 *   퇴근(checkOut*)이 null 이면 open 구간으로 본다(종료 = SENTINEL, 겹침 판정 시 +∞ 근사).
 */
public record DayAttdSegmentResult(
      String attdId
    , String workSeq
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
) {
}
