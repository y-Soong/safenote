package com.prafta.app.leave.leaveflow.application.command;

import java.math.BigDecimal;

/**
 * prafta-app-018-B: tb_user_attd_req(REQ_TYPE='05' 연차사용) INSERT 커맨드.
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.application.command.LeaveReqInsertCommand} 미러.
 * INSERT 는 이름 기반 {@code #{...}} 매핑이라 필드 순서는 위치매핑 함정 비해당.
 */
public record LeaveReqInsertCommand(
      String reqId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String reqStatus
    , String reqReason
    , String workYmd
    , String nodeCd
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String leaveType
    , BigDecimal leaveDays
    , String insertNo
    /**
     * prafta-leavemulti: 연차 기간(From-To) 신청 묶음 ID. 같은 신청에서 분해된 날짜별 REQ 가 동일 값.
     * <p><b>단일일 신청은 null</b> → 컬럼에 NULL 이 들어가 기존 데이터/조회와 동일하다.
     */
    , String leaveGroupId
) {
}
