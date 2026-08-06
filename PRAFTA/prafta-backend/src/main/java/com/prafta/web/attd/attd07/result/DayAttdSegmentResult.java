package com.prafta.web.attd.attd07.result;

/**
 * 근무일 ±1 윈도우의 근무 구간(WORK_SEQ) 출퇴근 시각 — 시각 겹침 판정(정책서 attd §7.6)용 최소 스냅샷.
 *
 * <p>{@code Attd07Mapper.selectAttdSegmentsAroundDayExcept} 결과. 편집/등록 대상 ATTD_ID 를 제외한
 *   같은 (회사/사업장/사용자) 활성(DEL_YN='N') 근태행의 근무일·차수·출퇴근 시각만 담는다.
 *   퇴근(checkOut*)이 null 이면 open(미마감) 구간으로 본다.
 *
 * <p><b>★ record 컴포넌트 순서 = SELECT 컬럼 순서</b>(MyBatis record 생성자 매핑은 컬럼 순서를 따른다).
 *   {@code Attd07Mapper.xml} 의 SELECT 순서(attdId, workYmd, workSeq, checkInDate, checkInTime,
 *   checkOutDate, checkOutTime)와 반드시 함께 유지할 것 — 불일치 시 무증상 오매핑이 발생한다.
 *
 * <p>{@code workYmd} 는 겹침가드 개선(2026-08-06)에서 추가되었다. 이웃 근무일 open 구간의 clamp
 *   (dayOffset 산출)와 원인별 안내 문구("몇 월 며칠 몇 차 근무")에 필요하다.
 */
public record DayAttdSegmentResult(
      String attdId
    , String workYmd
    , String workSeq
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
) {
}
