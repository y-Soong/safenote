package com.prafta.web.attd.attd15.result;

/**
 * ATTD15-T1 - 사용자별 COMPLETED 초과근무 분 합계(해당 주).
 *
 * <p>{@code Attd11Mapper.selectOvertimeSummary} 를 주간 범위(WORK_YMD BETWEEN)로 이식.
 * OT_STATUS='COMPLETED' AND DEL_YN='N' 만 합산(Attd11 decisions §3-4 동일 기준).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record OvertimeSummaryResult(
        String userCd
        , long otMinutes
) {
}
