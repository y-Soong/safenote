package com.prafta.web.acct.acct01.result;

/**
 * 근태 연계 조회 결과 VO (당일 실근태 1행 = WORK_SEQ 단위).
 * 스케줄(정규직)은 별도 scheduleResult 로 동봉하며, 본 VO 는 실근태 기록을 담는다.
 * PII(휴대폰) 미포함.
 */
public record AttendanceLinkResult(
    String attdId
    , String userCd
    , String workYmd
    , Integer workSeq
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
){
}
