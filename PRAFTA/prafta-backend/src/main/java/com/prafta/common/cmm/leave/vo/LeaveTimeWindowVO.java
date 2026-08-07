package com.prafta.common.cmm.leave.vo;

/**
 * 그날 "시각 보유" 연차(반차 '01' + 시간차 '02'~'04') 1건의 시각 구간 (sec N-2, 2026-08-07).
 *
 * <p>연차 시간대 겹침 판정(ATTD_400_112)을 SQL CASE 에서 Java 로 이관하면서 쓰는 운반체다.
 * 종전 SQL 은 <b>각 행이 자기 {@code END_TIME < START_TIME} 일 때만</b> +1일 보정해서, 한쪽만 wrap
 * 되는 조합(야간 시작기준 반차 {@code '2200'~'0200'} vs 시간차 {@code '0030'~'0130'})에서 프레임이
 * 어긋나 겹침을 놓쳤다. 환산은 {@code PartialLeaveWindowUtils.exemptStampRange}(그날 원 스케줄을
 * 프레임으로 정렬) 단일 진입점으로만 수행한다.
 *
 * <p>연차 행은 {@code START_DATE = END_DATE = 근무일} 고정이므로 일자 컬럼은 담지 않는다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 *
 * @param startTime 연차 시작 시각(HHmm)
 * @param endTime   연차 종료 시각(HHmm)
 */
public record LeaveTimeWindowVO(
      String startTime
    , String endTime
) {
}
