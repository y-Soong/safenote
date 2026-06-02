package com.prafta.app.home.home01.result;

/**
 * prafta-app-001: 오늘 근태(tb_user_attd_mgmt) 1건 조회 결과.
 * <p>매핑(AppHome01Mapper.selectTodayAttd):
 * <pre>
 *   CHECK_IN_TIME  AS checkInTime   (varchar4 HHMM)
 *   CHECK_OUT_TIME AS checkOutTime  (varchar4 HHMM, null 허용)
 *   WORK_SEQ       AS workSeq
 * </pre>
 * 레코드 자체가 없으면 서비스에서 BEFORE_WORK 로 처리한다.
 */
public record AttdMgmtResult(
    String checkInTime
    , String checkOutTime
    , Integer workSeq
) {
}
