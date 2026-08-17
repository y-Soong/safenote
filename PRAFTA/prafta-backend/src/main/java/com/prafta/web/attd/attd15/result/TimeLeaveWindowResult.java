package com.prafta.web.attd.attd15.result;

/**
 * A안(2026-08-17): 조회 주간 안의 확정 "시각 보유" 연차(반차 01 + 시간차 02/03/04) 구간 1건.
 *
 * <p>연차 사용 시간은 유급이되 근로시간이 아니므로(주52·연장 판정은 실근로 기준),
 * 실제기준(raw) 합산에서 실근태와의 겹침만큼 차감하는 데 쓴다.
 * 시각 표기는 저장 규약(END_TIME &lt;= START_TIME 이면 익일 wrap)을 따른다.
 *
 * <p>★ record 위치 기반 매핑 — SELECT 컬럼 순서와 컴포넌트 순서 1:1 유지.
 */
public record TimeLeaveWindowResult(
      String userCd
    , String workYmd
    , String startTime
    , String endTime
) {
}
