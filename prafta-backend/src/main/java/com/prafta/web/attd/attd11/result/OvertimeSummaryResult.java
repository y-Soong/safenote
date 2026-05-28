package com.prafta.web.attd.attd11.result;

/**
 * PRAFTA-034 - Attd_11 사용자별 초과근무 분 합계.
 *
 * TB_USER_OVERTIME_MGMT 에서 OT_STATUS='COMPLETED' AND DEL_YN='N' 인
 * WORK_MINUTES 를 해당 월 USER_CD 단위로 SUM 한 값. decisions §3-4 / §4.
 */
public record OvertimeSummaryResult(
        String userCd
        , String userId
        , String userNm
        , String deptNm
        , String authCd
        , String authNm
        , long otMinutes
) {
}
